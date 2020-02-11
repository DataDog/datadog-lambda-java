package com.datadoghq.datadog_lambda_java;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
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
            DDLogger.getLoggerImpl().debug("Cannot submit a fake span on a null context. Is the DD tracing context being initialized correctly?");
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

    public void setStart_time(Double start_time) {
        this.start_time = start_time;
    }

    public void setEnd_time(Double end_time) {
        this.end_time = end_time;
    }
    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public void setTrace_id(String trace_id) {
        this.trace_id = trace_id;
    }

    private String name;
    private String id;
    private Double start_time;
    private Double end_time;
    private String type;
    private String parent_id;
    private String trace_id;
    private DDTraceContext ddt;

    public ErsatzSegment(DDTraceContext ctx){
        this.start_time = ((double) new Date().getTime()) /1000d;
        this.name = "datadog-metadata";
        this.type = "subsegment";

        byte[] idBytes = new byte[8];
        Random rnd = new Random();
        rnd.nextBytes(idBytes);
        this.id = "";
        for (byte b : idBytes){
            this.id = this.id + String.format("%02x", b);
        }

        //Root=1-5e41a79d-e6a0db584029dba86a594b7e;Parent=8c34f5ad8f92d510;Sampled=1
        String traceId = System.getenv("_X_AMZN_TRACE_ID");
        if (traceId != null) {
            String[] traceParts = traceId.split(";");
            this.trace_id = traceParts[0].split("=")[1];
            this.parent_id = traceParts[1].split("=")[1];
        } else {
            DDLogger.getLoggerImpl().error("Unable to find trace context _X_AMZN_TRACE_ID in the environment variables");
        }
        this.end_time = ((double) new Date().getTime()) /1000d;

        this.ddt = ctx;
    }

    public String toJSON(){
        JSONObject dd = new JSONObject();
        JSONObject tr = new JSONObject();

        tr.put("trace", ddt.toJSON());
        dd.put("datadog", tr);

        JSONObject wholeThing = new JSONObject()
                .put("name", this.name)
                .put("id", this.id)
                .put("start_time", this.start_time)
                .put("end_time", this.end_time)
                .put("metadata", dd)
                .put("type", this.type)
                .put("parent_id", this.parent_id)
                .put("trace_id", this.trace_id);

        DDLogger.getLoggerImpl().debug(wholeThing.toString());
        System.out.println(wholeThing.toString());
        return wholeThing.toString();
    }

    public boolean sendToXRay(){
        if (this.id == null || this.id == "") {
            return false;
        }

        String s_daemon_ip;
        String s_daemon_port;
        String daemon_address_port = System.getenv("AWS_XRAY_DAEMON_ADDRESS");
        if (daemon_address_port != null) {
            System.out.println("XRay daemon env var is " + daemon_address_port);
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

        JSONObject prefix  = new JSONObject()
                .put("format", "json")
                .put("version", 1);
        String s_message = this.toJSON();
        String s_payload = prefix.toString() + "\n" + s_message;

        byte[] payload = s_payload.getBytes();
        DatagramPacket packet = new DatagramPacket(payload, payload.length, daemon_address, daemon_port);

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
        headers = toLowerKeys(headers);

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
            headers.put(ddSamplingKey, "1");
        }
        this.samplingPriority = headers.get(ddSamplingKey);
    }

    private  Map<String,String> toLowerKeys(Map<String,String> headers){
        Map<String, String> headers2  = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            headers2.put(k,v);
            headers2.put(k.toLowerCase(), v);
        }
        return headers2;
    }

    public String toString(){
        JSONObject jo = this.toJSON();
        return jo.toString();
    }

    public JSONObject toJSON(){
        JSONObject jo = new JSONObject();
        jo.put("trace-id", this.getTraceID());
        jo.put("parent-id", this.getParentID());
        jo.put("sampling-priority", this.getSamplingPriority());
        return jo;
    }
}
