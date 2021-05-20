package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KinesisHeaderable extracts DD's attributes from Kinesis Events. The conventions are mirrored to those
 * defined in datadog-lambda-js library.
 */
public class KinesisHeaderable implements Headerable {

    public static final String DATADOG_ATTRIBUTE_NAME = "_datadog";

    private Map<String, String> headers;

    public KinesisHeaderable(KinesisEvent event) {
        if (event != null &&
                (event.getRecords() != null) &&
                !event.getRecords().isEmpty() &&
                (event.getRecords().get(0).getKinesis() != null) &&
                (event.getRecords().get(0).getKinesis().getData() != null)) {
            ByteBuffer firstRecordPayload = event.getRecords().get(0).getKinesis().getData();
            Gson g = new Gson();
            try {
                String payloadStr = new String(firstRecordPayload.array(), StandardCharsets.UTF_8);
                KinesisPayload payload = g.fromJson(payloadStr, KinesisPayload.class);
                if (payload == null || payload.datadogTracingInfo == null) {
                    this.headers = Collections.emptyMap();
                } else {
                    this.headers = payload.datadogTracingInfo.asMap();
                }
            } catch (JsonSyntaxException jse) {
                this.headers = Collections.emptyMap();
            }
        } else {
            this.headers = Collections.emptyMap();
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    private static class KinesisPayload {

        @SerializedName(value = "_datadog")
        private DatadogTracingInfo datadogTracingInfo;
    }

    private static class DatadogTracingInfo {

        @SerializedName(value = "x-datadog-trace-id")
        private String traceID;
        @SerializedName(value = "x-datadog-parent-id")
        private String parentID;
        @SerializedName(value = "x-datadog-sampling-priority")
        private String samplingPriority;

        Map<String, String> asMap() {
            if (traceID == null || parentID == null) {
                return Collections.emptyMap();
            }
            Map<String, String> tracingInfo = new HashMap<>();
            tracingInfo.put("x-datadog-trace-id", traceID);
            tracingInfo.put("x-datadog-parent-id", parentID);
            tracingInfo.put("x-datadog-sampling-priority", samplingPriority == null ? "2" : samplingPriority);

            return Collections.unmodifiableMap(tracingInfo);
        }
    }
}
