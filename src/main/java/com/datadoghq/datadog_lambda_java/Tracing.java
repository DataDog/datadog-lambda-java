package com.datadoghq.datadog_lambda_java;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;
import datadog.trace.api.CorrelationIdentifier;

public class Tracing{

    protected transient DDTraceContext cxt;
    protected transient XRayTraceContext xrt;
    protected static final String TRACE_ID_KEY = "dd.trace_id";
    protected static final String SPAN_ID_KEY = "dd.span_id";

    public Tracing(){
        this.xrt = new XRayTraceContext();
    }

    public Tracing(APIGatewayV2ProxyRequestEvent req){
        this.cxt = populateDDContext(req.getHeaders());
        this.xrt = new XRayTraceContext();
    }

    public Tracing(APIGatewayProxyRequestEvent req){
        this.cxt = populateDDContext(req.getHeaders());
        this.xrt = new XRayTraceContext();
    }

    public Tracing(Headerable req){
        this.cxt = populateDDContext(req.getHeaders());
        this.xrt = new XRayTraceContext();
    }

    /**
     * Test constructor that can take a dummy _X_AMZN_TRACE_ID value
     * @param xrayTraceInfo is a dummy _X_AMZN_TRACE_ID used for testing
     */
    protected Tracing(String xrayTraceInfo){
        this.xrt = new XRayTraceContext(xrayTraceInfo);
    }


    public DDTraceContext getDDContext() {
        if (this.cxt == null) {
            return new DDTraceContext();
        }
        return this.cxt;
    }

    public XRayTraceContext getXrayContext() {
        if (this.xrt == null){
            return new XRayTraceContext();
        }
        return xrt;
    }

    public Map<String,String> getLogCorrelationTraceAndSpanIDsMap(){
        String traceId = String.valueOf(CorrelationIdentifier.getTraceId());
        if (traceId != null && ! (traceId.equals("") || traceId.equals("0") )){
            String spanId = String.valueOf(CorrelationIdentifier.getSpanId());
            Map<String, String> out  = new HashMap<String, String>();
            out.put(TRACE_ID_KEY, traceId);
            out.put(SPAN_ID_KEY, spanId);
            return out;
        }
        if (this.cxt != null){
            traceId = this.cxt.getTraceID();
            String spanId = this.cxt.getParentID();
            Map<String, String> out  = new HashMap<String, String>();
            out.put(TRACE_ID_KEY, traceId);
            out.put(SPAN_ID_KEY, spanId);
            return out;
        }
        if (this.xrt != null){
            traceId = this.xrt.getAPMTraceID();
            String spanId = this.xrt.getAPMParentID();
            Map<String, String> out  = new HashMap<String, String>();
            out.put(TRACE_ID_KEY, traceId);
            out.put(SPAN_ID_KEY, spanId);
            return out;
        }
        DDLogger.getLoggerImpl().debug("No DD trace context or XRay trace context set!");
        return null;
    }

    private String formatLogCorrelation(String trace, String span){
        return String.format("[dd.trace_id=%s dd.span_id=%s]", trace, span);
    }

    private static DDTraceContext populateDDContext(Map<String,String> headers){
        try{
            return new DDTraceContext(headers);
        } catch (Exception e) {
            DDLogger.getLoggerImpl().debug("Unable to extract DD Trace Context from event headers");
        }
        return null;
    }


    protected boolean submitSegment(){
        if(this.cxt == null){
            DDLogger.getLoggerImpl().debug("Cannot submit a fake span on a null context. Is the DD tracing context being initialized correctly?");
            return false;
        }

        ConverterSubsegment es = new ConverterSubsegment(this.cxt, this.xrt);
        return es.sendToXRay();
    }


    protected Map<String,String> makeOutboundHttpTraceHeaders(){
        Map<String, String> traceHeaders  = new HashMap<String, String>();
        if(this.cxt == null
                || this.xrt == null
                || this.cxt.getTraceID() == null
                || this.cxt.getSamplingPriority() == null
                || this.xrt.getAPMParentID() == null){
            DDLogger.getLoggerImpl().debug("Cannot make outbound trace headers -- some required fields are null");
            return traceHeaders;
        }

        traceHeaders.put(DDTraceContext.ddTraceKey, this.cxt.getTraceID());
        traceHeaders.put(DDTraceContext.ddSamplingKey, this.cxt.getSamplingPriority());
        traceHeaders.put(DDTraceContext.ddParentKey, this.xrt.getAPMParentID());

        return traceHeaders;
    }

    protected String makeOutboundJson(){
        Map<String, String> headers = makeOutboundHttpTraceHeaders();
        Gson g = new Gson();
        return g.toJson(headers);
    }
}



class ConverterSubsegment {
    public void setId(String id) {
        this.id = id;
    }
    public void setStart_time(Double start_time) {
        this.start_time = start_time;
    }
    public void setEnd_time(Double end_time) {
        this.end_time = end_time;
    }

    private transient String name;
    private transient String id;
    private transient Double start_time;
    private transient Double end_time;
    private transient String type;
    private transient DDTraceContext ddt;
    private transient XRayTraceContext xrt;

    private static final int EXPECTED_ENDPOINT_PARTS = 2;

    public ConverterSubsegment(DDTraceContext ctx, XRayTraceContext xrt){
        this.start_time = ((double) new Date().getTime()) /1000d;
        this.name = "datadog-metadata";
        this.type = "subsegment";

        byte[] idBytes = new byte[8];
        TracingRandom.getRandom().nextBytes(idBytes);
        this.id = "";
        for (byte b : idBytes){
            this.id = this.id + String.format("%02x", b);
        }
        this.end_time = ((double) new Date().getTime()) /1000d;

        this.ddt = ctx;
        this.xrt = xrt;
    }

    public String toJSONString(){

        XraySubsegment.XraySubsegmentBuilder xrb = new XraySubsegment.XraySubsegmentBuilder();
        XraySubsegment xrs = xrb.name(this.name)
                .id(this.id)
                .startTime(this.start_time)
                .endTime(this.end_time)
                .type(this.type)
                .parentId(this.xrt.getParentId())
                .traceId(this.xrt.getTraceId())
                .ddTraceId(this.ddt.getTraceID())
                .ddSamplingPriority(this.ddt.getSamplingPriority())
                .ddParentId(this.ddt.getParentID())
                .build();

        Gson g = new Gson();
        return g.toJson(xrs);
    }

    public boolean sendToXRay(){
        if (this.id == null || this.id.equals("")) {
            return false;
        }

        String s_daemon_ip;
        String s_daemon_port;
        String daemon_endpoint = System.getenv("AWS_XRAY_DAEMON_ADDRESS");
        if (daemon_endpoint != null){
            if (daemon_endpoint.split(":").length != EXPECTED_ENDPOINT_PARTS){
                DDLogger.getLoggerImpl().error("Unexpected AWS_XRAY_DAEMON_ADDRESS value: ", daemon_endpoint);
                return false;
            }
            s_daemon_ip = daemon_endpoint.split(":")[0];
            s_daemon_port = daemon_endpoint.split(":")[1];
            DDLogger.getLoggerImpl().debug("AWS XRay Address: ", s_daemon_ip);
            DDLogger.getLoggerImpl().debug("AWS XRay Port: ", s_daemon_port);
        } else {
            DDLogger.getLoggerImpl().error("Unable to get AWS_XRAY_DAEMON_ADDRESS from environment vars");
            return false;
        }

        try {
            InetAddress daemon_address = InetAddress.getByName(s_daemon_ip);
            int daemon_port = Integer.parseInt(s_daemon_port);

            Map<String, Object> prefixMap  = new HashMap<String, Object>();
            prefixMap.put("format", "json");
            prefixMap.put("version", 1);

            String s_message = this.toJSONString();

            Gson g = new Gson();
            String s_payload = g.toJson(prefixMap) + "\n" + s_message;

            byte[] payload = s_payload.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(payload, payload.length, daemon_address, daemon_port);
            DatagramSocket socket;
            socket = new DatagramSocket();
            try {
                socket.send(packet);
            } catch (IOException e) {
                DDLogger.getLoggerImpl().error("Couldn't send packet! " + e.getMessage());
                return false;
            } finally {
                socket.close();
            }
        } catch (UnknownHostException e) {
            DDLogger.getLoggerImpl().error("Unexpected exception looking up the AWS_XRAY_DAEMON_ADDRESS. This address should be a dotted quad and not require host resolution.");
            return false;
        } catch (NumberFormatException ne) {
            DDLogger.getLoggerImpl().error("Excepting parsing daemon port" + ne.getMessage());
            return false;

        } catch (SocketException e) {
            DDLogger.getLoggerImpl().error("Unable to bind to an available socket! " + e.getMessage());
            return false;
        }
        return true;
    }

}

class DDTraceContext {
    protected transient String traceID;
    protected transient String parentID;
    protected transient String samplingPriority;

    public String getTraceID() {
        return traceID;
    }

    public String getParentID() {
        return parentID;
    }

    public String getSamplingPriority() {
        return samplingPriority;
    }

    protected static final String ddTraceKey = "x-datadog-trace-id";
    protected static final String ddParentKey = "x-datadog-parent-id";
    protected static final String ddSamplingKey = "x-datadog-sampling-priority";

    public DDTraceContext(){
    }

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
            DDLogger.getLoggerImpl().debug("Headers missing the DD Sampling Priority. Defaulting to '2'");
            headers.put(ddSamplingKey, "2");
        }
        this.samplingPriority = headers.get(ddSamplingKey);
    }

    private  Map<String,String> toLowerKeys(Map<String,String> headers){
        Map<String, String> lowerCaseHeaders  = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            lowerCaseHeaders.put(k,v);
            lowerCaseHeaders.put(k.toLowerCase(Locale.US), v);
        }
        return lowerCaseHeaders;
    }

    public Map<String, String> toJSONMap(){
        Map<String, String> jo  = new HashMap<String, String>();
        jo.put("trace-id", this.getTraceID());
        jo.put("parent-id", this.getParentID());
        jo.put("sampling-priority", this.getSamplingPriority());
        return jo;
    }

    public Map<String,String> getKeyValues(){
        Map<String, String> keyValues  = new HashMap<String, String>();
        if (this.traceID != null) {
            keyValues.put(ddTraceKey, this.traceID);
        }
        if (this.parentID != null) {
            keyValues.put(ddParentKey, this.parentID);
        }

        if (this.samplingPriority != null) {
            keyValues.put(ddSamplingKey, this.samplingPriority);
        }
        return keyValues;
    }
}


class XRayTraceContext{
    String traceIdHeader;
    String traceId;
    String parentId;

    public XRayTraceContext(){
        //Root=1-5e41a79d-e6a0db584029dba86a594b7e;Parent=8c34f5ad8f92d510;Sampled=1
        String traceId = System.getenv("_X_AMZN_TRACE_ID");
        if (traceId == null || traceId.equals("")){
            DDLogger.getLoggerImpl().debug("Unable to find _X_AMZN_TRACE_ID");
            return;
        }
        String[] traceParts = traceId.split(";");
        if(traceParts.length != 3){
            DDLogger.getLoggerImpl().error ("Malformed _X_AMZN_TRACE_ID value: "+ traceId);
            return;
        }

        try {
            this.traceId = traceParts[0].split("=")[1];
            parentId = traceParts[1].split("=")[1];
        } catch (Exception e){
            DDLogger.getLoggerImpl().error("Malformed _X_AMZN_TRACE_ID value: "+ traceId);
            return;
        }
        this.traceIdHeader = traceId;
    }

    /**
     * Test constructor that can take a dummy _X_AMZN_TRACE_ID value rather than reading from env vars
     * @param traceId
     */
    protected XRayTraceContext(String traceId){
        String[] traceParts = traceId.split(";");
        if(traceParts.length != 3){
            DDLogger.getLoggerImpl().error("Malformed _X_AMZN_TRACE_ID value: "+ traceId);
            return;
        }

        try {
            this.traceId = traceParts[0].split("=")[1];
            parentId = traceParts[1].split("=")[1];
        } catch (Exception e){
            DDLogger.getLoggerImpl().error("Malformed _X_AMZN_TRACE_ID value: "+ traceId);
            return;
        }
        this.traceIdHeader = traceId;
    }

    public void setTraceIdHeader(String traceIdHeader) {
        this.traceIdHeader = traceIdHeader;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getParentId() {
        return parentId;
    }
    public Map<String,String> getKeyValues(){
        Map<String, String> keyValues  = new HashMap<String, String>();
        if (this.traceIdHeader != null) {
            keyValues.put("X-Amzn-Trace-Id", this.traceIdHeader);
        }
        return keyValues;
    }

    public String getAPMParentID(){
        try {
            String lastSixteen = this.parentId.substring(this.parentId.length()-16);
            Long l_ApmId;
            l_ApmId = Long.parseUnsignedLong(lastSixteen, 16);
            return Long.toUnsignedString(l_ApmId);
        }catch (Exception e){
            DDLogger.getLoggerImpl().debug("Problem converting XRay Parent ID to APM Parent ID: "+ e.getMessage());
            return null;
        }
    }

    public String getAPMTraceID(){
        //trace ID looks like 1-5e41a79d-e6a0db584029dba86a594b7e
        String bigid = "";
        try {
            bigid = this.traceId.split("-")[2];
        }
        catch (ArrayIndexOutOfBoundsException | NullPointerException ai){
            DDLogger.getLoggerImpl().debug("Unexpected format for the trace ID. Unable to parse it. " + this.traceId);
            return "";
        }

        //just to verify
        if (bigid.length() != 24){
            DDLogger.getLoggerImpl().debug("Got an unusual traceid from x-ray. Unable to convert that to an APM id. " + this.traceId);
            return "";
        }

        String last16 = bigid.substring(bigid.length()-16); // should be the last 16 characters of the big id

        Long parsed = 0L;
        try {
            parsed = Long.parseUnsignedLong(last16, 16); //unsigned because parseLong throws a numberformatexception at anything greater than 0x7FFFF...
        }
        catch (NumberFormatException ne){
            DDLogger.getLoggerImpl().debug("Got a NumberFormatException trying to parse the traceID. Unable to convert to an APM id. " + this.traceId);
            return "";
        }
        parsed = parsed & 0x7FFFFFFFFFFFFFFFL; //take care of that pesky first bit...
        return parsed.toString();
    }
}

class TracingRandom{
    static final SecureRandom rnd = new SecureRandom();
    static SecureRandom getRandom(){
        return rnd;
    }
}