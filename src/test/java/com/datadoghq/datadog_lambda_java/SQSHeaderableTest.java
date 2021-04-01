package com.datadoghq.datadog_lambda_java;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class SQSHeaderableTest {

    @Test
    public void testParsingNullSQSEvent_returnsEmptyHeaders() {
        Map<String, String> headers = new SQSHeaderable(null).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingSQSEventNoRecords_returnsEmptyHeaders() {
        SQSEvent event = new SQSEvent();

        Map<String, String> headers = new SQSHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingSQSEventFirstRecordHasNoDatadogAttributes_returnsEmptyHeaders() {
        SQSEvent event = new SQSEvent();
        event.setRecords(singletonList(new SQSMessage()));

        Map<String, String> headers = new SQSHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingSQSEventWithRecordWithDatadogAttributes_returnsHeaders() {
        SQSEvent event = new SQSEvent();
        SQSMessage message = new SQSMessage();
        Map<String, MessageAttribute> attributes = new HashMap<>();
        MessageAttribute datadogMessageAttribute = new MessageAttribute();
        datadogMessageAttribute.setStringValue("{\"x-datadog-trace-id\": \"123\","
                + "\"x-datadog-parent-id\": \"456\","
                + "\"x-datadog-sampling-priority\": \"2\"}");
        attributes.put(SQSHeaderable.DATADOG_ATTRIBUTE_NAME, datadogMessageAttribute);
        message.setMessageAttributes(attributes);
        event.setRecords(singletonList(message));

        Map<String, String> headers = new SQSHeaderable(event).getHeaders();

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("x-datadog-trace-id", "123");
        expectedHeaders.put("x-datadog-parent-id", "456");
        expectedHeaders.put("x-datadog-sampling-priority", "2");

        Assert.assertEquals(expectedHeaders, headers);
    }

}
