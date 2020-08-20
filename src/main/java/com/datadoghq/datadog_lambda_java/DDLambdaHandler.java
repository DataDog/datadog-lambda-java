package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.xray.entities.TraceHeader;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import datadog.trace.api.DDTags;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.bytebuddy.agent.ByteBuddyAgent;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

public class DDLambdaHandler implements RequestStreamHandler {
    private static final String XRAY_ENV_VARIABLE = "_X_AMZN_TRACE_ID";
    private static final String DD_LAMBDA_VARIABLE = "DD_LAMBDA_HANDLER";
    private static final AtomicBoolean NEEDS_TO_ATTACH = new AtomicBoolean(true);
    private final String functionName;
    private final Tracer tracer;

    private final Method targetMethod;
    private final Object instance;

    private final boolean isStreamType;
    private JsonAdapter parameterMapper;
    private JsonAdapter resultMapper;

    public DDLambdaHandler() throws ReflectiveOperationException {
        // Setup agent
        System.setProperty("dd.jmxfetch.enabled", "false");

        if (System.getenv("DD_SERVICE_NAME") == null
                && System.getenv("DD_SERVICE") == null
                && System.getProperty("dd.service.name") == null
                && System.getProperty("dd.service") == null) {
            System.setProperty("dd.service", "aws.lambda");
        }

        if (NEEDS_TO_ATTACH.getAndSet(false)) {
            //how are we supposed to get the dd-java-agent.jar here?
            //TODO: meeting w/ APM Java about packaging this
            File agentJar = new File(System.getenv("LAMBDA_TASK_ROOT") + "/lib/dd-java-agent-0.60.1.jar");
            ByteBuddyAgent.attach(
                    agentJar,
                    ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE,
                    null,
                    ByteBuddyAgent.AttachmentProvider.ForEmulatedAttachment.INSTANCE);
        }

        functionName = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
        tracer = GlobalTracer.get();

        // Setup delegate
        String delegateClassName = System.getenv(DD_LAMBDA_VARIABLE);

        if (delegateClassName == null) {
            //TODO: log
            throw new IllegalArgumentException(DD_LAMBDA_VARIABLE + " not defined");
        }
        Class<?> delegateClass = Class.forName(delegateClassName);

        targetMethod = findHandlerMethod(delegateClass);
        instance = delegateClass.newInstance();

        if (RequestStreamHandler.class.isAssignableFrom(delegateClass)) {
            isStreamType = true;
        } else {
            isStreamType = false;
            parameterMapper =
                    new Moshi.Builder().build().adapter(targetMethod.getGenericParameterTypes()[0]).lenient();
            resultMapper =
                    new Moshi.Builder().build().adapter(targetMethod.getGenericReturnType()).lenient();
        }
    }

    private static Method findHandlerMethod(Class target) {
        for (Method method : target.getMethods()) {
            if (method.getName().equals("handleRequest")) {
                return method;
            }
        }

        throw new IllegalArgumentException(
                //TODO: log
                "Handler '" + target + "'does not implement 'handleRequest'");
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        DDLogger.getLoggerImpl().debug("Handling new request...");
        if (isStreamType) {
            invokeStreamType(inputStream, outputStream, context);
        } else {
            invokeHandlerType(inputStream, outputStream, context);
        }
    }

    private void invokeStreamType(
            InputStream inputStream, OutputStream outputStream, Context context) {
        Span span = buildSpan(Collections.emptyMap());
        DDLogger logger = DDLogger.getLoggerImpl();
        logger.debug("request appears to be an input stream");
        DDLambda ddl = new DDLambda(context);
        WrapperLocker.lock(); // prevent subsequent instantiations of DDLambda from recording invocation metrics

        try (Scope scope = tracer.activateSpan(span)) {
            targetMethod.invoke(instance, inputStream, outputStream, context);
        } catch (Throwable e) {
            logger.debug("Got an exception from the inner handler: ", e);
            ddl.error(context);
            span.setTag(Tags.ERROR, true);
            span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));

            throw new RuntimeException(e);
        } finally {
            WrapperLocker.unlock();
            span.finish();
        }
    }

    private void invokeHandlerType(
            InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Object parameter = parameterMapper.fromJson(new Buffer().readFrom(inputStream));

        DDLogger logger = DDLogger.getLoggerImpl();
        Map<String, String> headers = Collections.emptyMap();
        // TODO: make it so we only reflectively determine what sort of thing parameters is once, store result in singleton enum
        // Right now, parameters will always be a map<string, string>. We should be able to do one of two things:
        // - Either reflectively determine the input type of the customer's handler
        // - or take a best guess at the type of the handler by trying to gson deserialize it into different things
        // To discuss: create a bunch of abstract classes, one of which DDTraced handlers must implement?
        DDLambda ddl = new DDLambda(context);

        Span span = buildSpan(headers);
        try (Scope scope = tracer.activateSpan(span);
                BufferedSink sink = Okio.buffer(Okio.sink(outputStream))) {
            Object result = targetMethod.invoke(instance, parameter, context);
            resultMapper.toJson(sink, result);
        } catch (Throwable e) {
            logger.debug("Got an error thrown from the inner handler: ", e);
            ddl.error(context);
            span.setTag(Tags.ERROR, true);
            span.log(Collections.singletonMap(Fields.ERROR_OBJECT, e));

            throw new RuntimeException(e);
        } finally {
            span.finish();
        }
    }

    private Span buildSpan(Map<String, String> datadogHeaders) {
        Map<String, String> contextMap = new HashMap<>(datadogHeaders);

        String xrayId = System.getenv(XRAY_ENV_VARIABLE);
        if (xrayId != null) {
            TraceHeader traceHeader = TraceHeader.fromString(xrayId);

            // TODO implement the conversions here and uncomment
            // contextMap.putIfAbsent("x-datadog-trace-id", traceHeader.getRootTraceId().getNumber());
            // contextMap.putIfAbsent("x-datadog-sampling-priority", traceHeader.getSampled());

            // Not putIfAbsent because we want to overwrite datadog parent id
            // contextMap.put("x-datadog-parent-id", traceHeader.getParentId());
        }

        SpanContext spanContext =
                tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(contextMap));

        Span span =
                tracer
                        .buildSpan("aws.lambda")
                        .withTag(DDTags.RESOURCE_NAME, functionName)
                        .withTag(DDTags.SPAN_TYPE, "serverless")
                        .asChildOf(spanContext)
                        .start();

        return span;
    }
}

