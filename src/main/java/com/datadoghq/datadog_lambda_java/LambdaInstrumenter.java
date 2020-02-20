package com.datadoghq.datadog_lambda_java;

import java.net.URLConnection;
import java.util.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import okhttp3.Request;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * The Lambda Instrumenter is used for getting information about your Lambda into Datadog.
 */
public class LambdaInstrumenter {
    private String ENHANCED_PREFIX = "aws.lambda.enhanced.";
    private String INVOCATION = "invocations";
    private String ERROR = "errors";
    private Tracing tracing;

    /**
     * Create a new Lambda instrumenter given some Lambda context
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(Context cxt){
        this.tracing = new Tracing();
        recordEnhanced(INVOCATION, cxt);
    }

    /**
     * Create a trace-enabled Lambda instrumenter given an APIGatewayProxyRequestEvent and a Lambda context
     * @param req Your Datadog trace headers are pulled from the request and sent to XRay for consumption by the
     *            Datadog Xray Crawler
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(APIGatewayProxyRequestEvent req, Context cxt){
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
    }

    /**
     * Create a trace-enabled Lambda instrumenter given an APIGatewayV2ProxyEventRequest and a Lambda context
     * @param req Your Datadog trace headers are pulled from the request and sent to XRay for consumption by the
     *            Datadog Xray Crawler
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(APIGatewayV2ProxyRequestEvent req, Context cxt){
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
    }

    /**
     * Create a trace-enabled Lambda instrumenter given a custom request object. Please note that your custom request
     * object MUST implement Headerable.
     * @param req A custom request object that implements Headerable. Datadog trace headers are pulled from this request object.
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(Headerable req, Context cxt){
        recordEnhanced(INVOCATION, cxt);
        this.tracing = new Tracing(req);
        this.tracing.submitSegment();
    }

    /**
     * recordCustomMetric allows the user to record their own custom metric that will be sent to Datadog.
     * @param name The metric's name
     * @param value The metric's value
     * @param tags A map of tags to be assigned to the metric
     */
    public void recordCustomMetric(String name, double value, Map<String, Object> tags){
        new CustomMetric(name, value, tags).write();
    }

    /**
     * recordError increments the aws.lambda.enhanced.error metric in Datadog.
     * @param cxt The AWS Context provided to your handler
     */
    public void recordError(Context cxt){
        recordEnhanced(ERROR, cxt);
    }

    /**
     * Flushes any asynchronous metrics. Call this before exiting your handler.
     */
    public void flush(){
        MetricWriter.getMetricWriterImpl().flush();
    }

    private void recordEnhanced(String basename, Context cxt){
        String metricName = ENHANCED_PREFIX + basename;
        Map<String, Object> tags = null;
        tags = EnhancedMetric.makeTagsFromContext(cxt);
        new CustomMetric(metricName, 1,tags).write();
    }


    /**
     * Adds Datadog and Xray trace headers to a java.net.URLConnection, so you can trace downstream HTTP requests.
     * @param urlConnection the URLConnection that will have the trace headers added to it.
     * @return Returns a mutated URLConnection with the new trace headers.
     */
    public URLConnection addTraceHeaders(URLConnection urlConnection){
        if (this.tracing == null) {
            DDLogger.getLoggerImpl().error("Unable to add trace headers from an untraceable request. Did you pass LambdaInstrumenter a request?");
            return urlConnection;
        }

        Map<String,String> ddHeaderKVs = this.tracing.getDDContext().getKeyValues();
        ddHeaderKVs.forEach(urlConnection::setRequestProperty);

        Map<String,String> xrHeaderKVs = this.tracing.getXrayContext().getKeyValues();
        xrHeaderKVs.forEach(urlConnection::setRequestProperty);

        return urlConnection;
    }

    /**
     * Adds Datadog and XRay trace header to an org.apache.http.client.methods.HttpUriRequest, so you can trace downstream HTTP requests.
     * @param httpRequest the HttpUriRequest that will have the trace headers added to it.
     * @return Returns a mutated HttpUriRequest with the new trace headers.
     */
    public HttpUriRequest addTraceHeaders(HttpUriRequest httpRequest){
        if (this.tracing == null) {
            DDLogger.getLoggerImpl().error("Unable to add trace headers from an untraceable request. Did you pass LambdaInstrumenter a request?");
            return httpRequest;
        }

        Map<String,String> ddHeaderKVs = this.tracing.getDDContext().getKeyValues();
        ddHeaderKVs.forEach(httpRequest::setHeader);

        Map<String,String> xrHeaderKVs = this.tracing.getXrayContext().getKeyValues();
        xrHeaderKVs.forEach(httpRequest::setHeader);

        return httpRequest;
    }


    /**
     * Adds Datadog and XRay trace header to an OKHttp3 request, so you can trace downstream HTTP requests.
     * @param request The OKHttp3 Request that will have the trace headers added to it.
     * @return Returns a mutated OKHttp3 Request with the new trace headers.
     */
    public Request addTraceHeaders(Request request){
        if (this.tracing == null) {
            return request;
        }

        Map<String,String> ddHeaderKVs = this.tracing.getDDContext().getKeyValues();
        Map<String,String> xrHeaderKVs = this.tracing.getXrayContext().getKeyValues();

        Request.Builder rb = request.newBuilder();

        ddHeaderKVs.forEach(rb::addHeader);
        xrHeaderKVs.forEach(rb::addHeader);

        return rb.build();
    }

}
