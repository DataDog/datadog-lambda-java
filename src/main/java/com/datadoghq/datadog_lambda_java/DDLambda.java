package com.datadoghq.datadog_lambda_java;

import org.slf4j.MDC;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import okhttp3.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * The DDLambda instrumenter is used for getting information about your Lambda into Datadog.
 */
public class DDLambda {
    private String ENHANCED_ENV = "DD_ENHANCED_METRICS";
    private String ENHANCED_PREFIX = "aws.lambda.enhanced.";
    private String INVOCATION = "invocations";
    private String ERROR = "errors";
    private String MDC_TRACE_CONTEXT_FIELD = "dd.trace_context";
    private String JSON_TRACE_ID = "dd.trace_id";
    private String JSON_SPAN_ID = "dd.span_id";
    private Tracing tracing;
    private boolean enhanced = true;

    /**
     * Create a new DDLambda instrumenter given some Lambda context
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public DDLambda(Context cxt){
        this.tracing = new Tracing();
        this.enhanced = checkEnhanced();
        recordEnhanced(INVOCATION, cxt);
        addTraceContextToMDC();
    }

    /**
     * Testing only: create a DDLambda instrumenter with a given context and xrayTraceInfo
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     * @param xrayTraceInfo This would normally be the contents of the "_X_AMZN_TRACE_ID" env var
     */
    protected DDLambda(Context cxt, String xrayTraceInfo){
        this.tracing = new Tracing(xrayTraceInfo);
        this.enhanced = checkEnhanced();
        recordEnhanced(INVOCATION, cxt);
        addTraceContextToMDC();
    }

    /**
     * Create a trace-enabled DDLambda instrumenter given an APIGatewayProxyRequestEvent and a Lambda context
     * @param req Your Datadog trace headers are pulled from the request and sent to XRay for consumption by the
     *            Datadog Xray Crawler
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public DDLambda(APIGatewayProxyRequestEvent req, Context cxt){
        this.enhanced = checkEnhanced();
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
        addTraceContextToMDC();
    }

    /**
     * Create a trace-enabled DDLambda instrumenter given an APIGatewayV2ProxyEventRequest and a Lambda context
     * @param req Your Datadog trace headers are pulled from the request and sent to XRay for consumption by the
     *            Datadog Xray Crawler
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public DDLambda(APIGatewayV2ProxyRequestEvent req, Context cxt){
        this.enhanced = checkEnhanced();
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
        addTraceContextToMDC();
    }

    /**
     * Create a trace-enabled DDLambda instrumenter given a custom request object. Please note that your custom request
     * object MUST implement Headerable.
     * @param req A custom request object that implements Headerable. Datadog trace headers are pulled from this request object.
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public DDLambda(Headerable req, Context cxt){
        this.enhanced = checkEnhanced();
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
        addTraceContextToMDC();
    }

    private void addTraceContextToMDC(){
        Map<String,String> traceContext = getTraceContext();
        String traceId = traceContext.get(tracing.TRACE_ID_KEY);
        String spanId = traceContext.get(tracing.SPAN_ID_KEY);

        //to make life easier for people using JSON logging
        MDC.put(JSON_TRACE_ID, traceId);
        MDC.put(JSON_SPAN_ID, spanId);
        MDC.put(MDC_TRACE_CONTEXT_FIELD, getTraceContextString());
    }

    protected boolean checkEnhanced(){
        String sysEnhanced = System.getenv(ENHANCED_ENV);
        if (sysEnhanced == null){
            return true;
        }

        if (sysEnhanced.toLowerCase().equals("false")){
            return false;
        }
        return true;
    }

    /**
     * metric allows the user to record their own custom metric that will be sent to Datadog.
     * @param name The metric's name
     * @param value The metric's value
     * @param tags A map of tags to be assigned to the metric
     */
    public void metric(String name, double value, Map<String, Object> tags){
        new CustomMetric(name, value, tags).write();
    }

    /**
     * metric allows the user to record their own custom metric that will be sent to Datadog.
     * also allows user to set his/her own date.
     * @param name The metric's name
     * @param value The metric's value
     * @param tags A map of tags to be assigned to the metric
     * @param date The date under which the metric value will appear in datadog
     */
    public void metric(String name, double value, Map<String, Object> tags, Date date){
        new CustomMetric(name, value, tags, date).write();
    }

    /**
     * error increments the aws.lambda.enhanced.error metric in Datadog.
     * @param cxt The AWS Context provided to your handler
     */
    public void error(Context cxt){
        recordEnhanced(ERROR, cxt);
    }

    private void recordEnhanced(String basename, Context cxt){
        String metricName = basename;
        Map<String, Object> tags = null;
        if (this.enhanced) {
            metricName = ENHANCED_PREFIX + basename;
            tags = EnhancedMetric.makeTagsFromContext(cxt);
        }
        new CustomMetric(metricName, 1,tags).write();
    }

    /**
     * openConnection calls openConnection on the provided URL, adds the Datadog trace headers, and then returns the
     * resulting URLConnection. This duplicates the usual workflow with a java.net.URLConnection
     * @param url the URL to which you want to open a connection.
     * @return a newly opened URLConnection with Datadog trace headers.
     * @throws IOException
     */
    public URLConnection makeUrlConnection(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        return addTraceHeaders(uc);
    }


    /**
     * Adds Datadog trace headers to a java.net.URLConnection, so you can trace downstream HTTP requests.
     * @param urlConnection the URLConnection that will have the trace headers added to it.
     * @return Returns a mutated URLConnection with the new trace headers.
     */
    public URLConnection addTraceHeaders(URLConnection urlConnection){
        if (this.tracing == null) {
            DDLogger.getLoggerImpl().error("Unable to add trace headers from an untraceable request. Did you pass LambdaInstrumenter a request?");
            return urlConnection;
        }

        Map<String,String> traceHeaders = tracing.makeOutboundHttpTraceHeaders();
        traceHeaders.forEach(urlConnection::setRequestProperty);

        return urlConnection;
    }

    /**
     * Creates an Apache HttpGet instrumented with Datadog trace headers. Please note, this does not _execute_ the HttpGet,
     * it merely creates a default HttpGet with headers added.
     * @param url The URL that will eventually be gotten.
     * @return Returns an HttpGet for the provided URL, instrumented with Datadog trace headers.
     */
    public HttpGet makeHttpGet(String url){
        HttpGet hg = new HttpGet(url);
        hg = (HttpGet) addTraceHeaders(hg);
        return hg;
    }

    /**
     * Adds Datadog trace header to an org.apache.http.client.methods.HttpUriRequest, so you can trace downstream HTTP requests.
     * @param httpRequest the HttpUriRequest that will have the trace headers added to it.
     * @return Returns a mutated HttpUriRequest with the new trace headers.
     */
    public HttpUriRequest addTraceHeaders(HttpUriRequest httpRequest){
        if (this.tracing == null) {
            DDLogger.getLoggerImpl().error("Unable to add trace headers from an untraceable request. Did you pass LambdaInstrumenter a request?");
            return httpRequest;
        }

        Map<String,String> traceHeaders = tracing.makeOutboundHttpTraceHeaders();
        traceHeaders.forEach(httpRequest::setHeader);

        return httpRequest;
    }

    /**
     * Create an OKHttp3 request builder with Datadog headers already added.
     * @return Returns an OKHttp3 Request Builder with Datadog trace headers already added.
     */
    public Request.Builder makeRequestBuilder(){
        Request.Builder hrb = new Request.Builder();

        Map<String,String> traceHeaders = tracing.makeOutboundHttpTraceHeaders();
        traceHeaders.forEach(hrb::addHeader);
        return hrb;
    }

    /**
     * Adds Datadog trace header to an OKHttp3 request, so you can trace downstream HTTP requests.
     * @param request The OKHttp3 Request that will have the trace headers added to it.
     * @return Returns a mutated OKHttp3 Request with the new trace headers.
     */
    public Request addTraceHeaders(Request request){
        if (this.tracing == null) {
            DDLogger.getLoggerImpl().error("Unable to add trace headers from an untraceable request. Did you pass LambdaInstrumenter a request?");
            return request;
        }
        Map<String,String> traceHeaders = tracing.makeOutboundHttpTraceHeaders();

        Request.Builder rb = request.newBuilder();
        traceHeaders.forEach(rb::addHeader);

        return rb.build();
    }

    /**
     * Get the trace context for trace/log correlation. Inject this into your logs in order to correlate logs with traces.
     * @return a map of the current trace context
     */
    public Map<String,String> getTraceContext(){
        if (this.tracing == null){
            DDLogger.getLoggerImpl().debug("No tracing context; unable to get Trace ID");
            return null;
        }
        return this.tracing.getLogCorrelationTraceAndSpanIDsMap();
    }

    /**
     * Get the trace context in string form. Inject this into your logs in order to correlate logs with traces.
     * @return a string representation of the current trace context
     */
    public String getTraceContextString(){
        Map<String,String> traceInfo = getTraceContext();
        if (traceInfo == null){
            DDLogger.getLoggerImpl().debug("No Trace/Log correlation IDs returned");
            return "";
        }

        String traceID = traceInfo.get(this.tracing.TRACE_ID_KEY);
        String spanID = traceInfo.get(this.tracing.SPAN_ID_KEY);
        return formatTraceContext(this.tracing.TRACE_ID_KEY, traceID, this.tracing.SPAN_ID_KEY, spanID);
    }

    private String formatTraceContext(String traceKey, String trace, String spanKey, String span){
        return String.format("[%s=%s %s=%s]", traceKey, trace, spanKey, span);
    }

}
