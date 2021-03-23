package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * SQSHeaderable extracts DD's attributes from SQS Events. The conventions are mirrored to those
 * defined in datadog-lambda-js library. 
 */
public class SQSHeaderable implements Headerable {

    public static final String DATADOG_ATTRIBUTE_NAME = "_datadog";
    private static final Type HEADERS_GSON_TYPE =  new TypeToken<Map<String, String>>(){}.getType();

    private Map<String, String> headers;

    /**
     * Given an SQS Event without any records or datadog attributes produces an empty Headerable.
     * Otherwise - look up the first record and extract the attributes from it. 
     * @param event SQS Event
     */
    public SQSHeaderable(SQSEvent event) {
        if (event != null &&
                (event.getRecords() != null) &&
                !event.getRecords().isEmpty() &&
                (event.getRecords().get(0).getMessageAttributes() != null) &&
                !event.getRecords().get(0).getMessageAttributes().isEmpty() &&
                !(event.getRecords().get(0).getMessageAttributes().get(DATADOG_ATTRIBUTE_NAME) == null)) {
            String datadogAttribute = event.getRecords().get(0).getMessageAttributes().get(DATADOG_ATTRIBUTE_NAME)
                    .getStringValue();
            Gson g = new Gson();
            this.headers = g.fromJson(datadogAttribute, HEADERS_GSON_TYPE);
        } else {
            this.headers = Collections.emptyMap();
        }
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
