package com.datadoghq.datadog_lambda_java;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import org.json.JSONObject;


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

    public boolean submitSegment(){
        if(this.ctx == null){
            DDLogger.getLoggerImpl().debug("Cannot submit a dummy span on a null context. Is the DD tracing context being initialized correctly?");
            return false;
        }

        ErsatzSegment es = new ErsatzSegment(ctx);
        return es.sendToXRay();
    }

}


class ErsatzSegment {
    public void setId(String id) {
        this.id = id;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public void setTrace_id(String trace_id) {
        this.trace_id = trace_id;
    }

    public void setDdt(DDTraceContext ddt) {
        this.ddt = ddt;
    }

    private String name;
    private String id;
    private Long start_time;
    private boolean in_progress;
    private String type;
    private String parent_id;
    private String trace_id;
    private DDTraceContext ddt;

    public ErsatzSegment(){
        this.name = "datadog-trace-metadata";
        this.in_progress = true;
        this.type = "subsegment";
    }

    public ErsatzSegment(DDTraceContext ctx){
        this.name = "datadog-trace-metadata";
        this.start_time = new Date().getTime()/1000;
        this.in_progress = true;
        this.type = "subsegment";

        //Root=1-5e41a79d-e6a0db584029dba86a594b7e;Parent=8c34f5ad8f92d510;Sampled=1
        String traceId = System.getenv("_X_AMZN_TRACE_ID");
        if (traceId != null) {
            String[] traceParts = traceId.split(";");

            this.trace_id = traceParts[0].split("=")[1];
            this.parent_id = traceParts[1].split("=")[1];
            this.id = this.parent_id;
        } else {
            DDLogger.getLoggerImpl().error("Unable to find trace context _X_AMZN_TRACE_ID in the environment variables");
        }

        this.ddt = ctx;
    }

    public byte[] toJSONBytes(){
        JSONObject dd = new JSONObject();
        JSONObject tr = new JSONObject();

        tr.put("trace", ddt.toString());
        dd.put("datadog", tr);

        JSONObject wholeThing = new JSONObject()
                .put("name", this.name)
                .put("id", this.id)
                .put("start_time", this.start_time)
                .put("in_progress", this.in_progress)
                .put("metadata", dd)
                .put("type", this.type)
                .put("parent_id", this.parent_id)
                .put("trace_id", this.trace_id);

        DDLogger.getLoggerImpl().debug(wholeThing.toString());
        return wholeThing.toString().getBytes();
    }

    public boolean sendToXRay(){
        if (this.id == null || this.id == "") {
            return false;
        }

        String s_daemon_ip;
        String s_daemon_port;
        String daemon_address_port = System.getenv("AWS_XRAY_DAEMON_ADDRESS");
        if (daemon_address_port != null) {
            s_daemon_ip = daemon_address_port.split(":")[0];
            s_daemon_port = daemon_address_port.split(":")[1];
        } else {
            DDLogger.getLoggerImpl().error("Unable to get AWS_XRAY_DAEMON_ADDRESS from environment vars");
            return false;
        }

        InetAddress daemon_address;

        try {
            daemon_address = InetAddress.getByName(s_daemon_ip);
        } catch (UnknownHostException e) {
            DDLogger.getLoggerImpl().error("Unexpected exception looking up the AWS_XRAY_DAEMON_ADDRESS. This address should be a dotted quad and not require host resolution.");
            return false;
        }

        int daemon_port;
        try {
            daemon_port = Integer.parseInt(s_daemon_port);
        } catch (NumberFormatException ne) {
            DDLogger.getLoggerImpl().error("Excepting parsing daemon port" + ne.getMessage());
            return false;
        }

        byte[] payload = this.toJSONBytes();
        DatagramPacket packet = new DatagramPacket(payload, payload.length, daemon_address, daemon_port);//bytes, length, address, port

        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            DDLogger.getLoggerImpl().error("Unable to bind to an available socket! " + e.getMessage());
            return false;
        }
        try {
            socket.send(packet);
        } catch (IOException e) {
            DDLogger.getLoggerImpl().error("Couldn't send packet! " + e.getMessage());
            return false;
        }
        DDLogger.getLoggerImpl().debug("Ersatz segment sent");
        return true;
    }

}

class DDTraceContext {
    private String traceID;
    private String parentID;
    private String samplingPriority;

    public String getTraceID() {
        return traceID;
    }

    public String getParentID() {
        return parentID;
    }

    public String getSamplingPriority() {
        return samplingPriority;
    }


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


    public String toString(){
        JSONObject jo = new JSONObject();
        jo.put("trace-id", this.getTraceID());
        jo.put("parent-id", this.getParentID());
        jo.put("sampling-priority", this.getSamplingPriority());
        return jo.toString();
    }
}
