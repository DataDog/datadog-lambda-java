package com.datadoghq.datadog_lambda_java;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;


public class Tracing {

    private DDTraceContext ctx;

    public Tracing(APIGatewayV2ProxyRequestEvent req){
        this.ctx = getContextFromHeaders(req.getHeaders());
    }

    public Tracing(APIGatewayProxyRequestEvent req){
        this.ctx = getContextFromHeaders(req.getHeaders());
    }

    private static DDTraceContext getContextFromHeaders(Map<String,String> headers){
        DDTraceContext ctx = null;
        try{
            ctx = new DDTraceContext(headers);
        } catch (Exception e) {
            DDLogger.getLoggerImpl().debug("Unable to extract DD Trace Context from event headers");
        }
        return ctx;
    }

    public boolean submitDummySpan(){
        if(this.ctx == null){
            DDLogger.getLoggerImpl().debug("Cannot submit a dummy span on a null context. Is the DD tracing context being initialized correctly?");
            return false;
        }
        String xrayMetatadaDDTraceID = "trace-id";
        String xrayMetadataDDParentID = "parent-id";
        String xrayMetadataDDSamplingPrio = "sampling-priority";

        Segment s = AWSXRay.beginDummySegment();

        s.putMetadata(xrayMetatadaDDTraceID, this.ctx.getTraceID());
        s.putMetadata(xrayMetadataDDParentID, this.ctx.getParentID());
        s.putMetadata(xrayMetadataDDSamplingPrio, this.ctx.getSamplingPriority());

        return AWSXRay.sendSegment(s);
    }

}

class DDTraceContext {
    private String traceID;
    private String parentID;

    public String getTraceID() {
        return traceID;
    }

    public String getParentID() {
        return parentID;
    }

    public String getSamplingPriority() {
        return samplingPriority;
    }

    private String samplingPriority;

    private String ddTraceKey = "x-datadog-trace-id";
    private String ddParentKey = "x-datadog-parent-id";
    private String ddSamplingKey = "x-datadog-sampling-priority";

    public DDTraceContext(Map<String, String> headers) throws Exception{
        if (headers == null) {
            DDLogger.getLoggerImpl().debug("Unable to extract DD Context from null headers");
            throw new Exception("null headers!");
        }

        if (headers.get(ddTraceKey) == null) {
            DDLogger.getLoggerImpl().debug("Headers missing the DD Trace ID");
            throw new Exception("No trace ID");
        }
        this.traceID = headers.get(ddTraceKey);

        if (headers.get(ddParentKey) == null) {
            DDLogger.getLoggerImpl().debug("Headers missing the DD Parent ID");
            throw new Exception("Missing Parent ID");
        }
        this.parentID = headers.get(ddParentKey);

        if (headers.get(ddSamplingKey) == null){
            DDLogger.getLoggerImpl().debug("Headers missing the DD Sampling Priority");
            throw new Exception("Missing Sampling Priority");
        }
        this.samplingPriority = headers.get(ddSamplingKey);
    }
}
