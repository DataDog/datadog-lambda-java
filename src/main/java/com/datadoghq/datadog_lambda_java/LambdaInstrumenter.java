package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.*;

/**
 * The Lambda Instrumenter is used for getting information about your Lambda into Datadog.
 */
public class LambdaInstrumenter {
    private String ENHANCED_PREFIX = "aws.lambda.enhanced.";
    private String INVOCATION = "invocations";
    private String ERROR = "errors";

    /**
     * Create a new Lambda instrumenter given some Lambda context
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(Context cxt){
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
        DDLogger.getLoggerImpl().debug("Segment submission status: ", new Tracing(req).submitSegment());
    }

    /**
     * Create a trace-enabled Lambda instrumenter given an APIGatewayV2ProxyEventRequest and a Lambda context
     * @param req Your Datadog trace headers are pulled from the request and sent to XRay for consumption by the
     *            Datadog Xray Crawler
     * @param cxt Enhanced Metrics pulls information from the Lambda context.
     */
    public LambdaInstrumenter(APIGatewayV2ProxyRequestEvent req, Context cxt){
        DDLogger.getLoggerImpl().error("Test error");
        DDLogger.getLoggerImpl().debug("Test debug");
        recordEnhanced(INVOCATION, cxt);
        DDLogger.getLoggerImpl().debug("Segment submission status: ", new Tracing(req).submitSegment());
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
}
