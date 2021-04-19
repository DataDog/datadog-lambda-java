package com.datadoghq.datadog_lambda_java;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class KinesisHeaderableTest {

    @Test
    public void testParsingNullKinesisEvent_returnsEmptyHeaders() {
        Map<String, String> headers = new KinesisHeaderable(null).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingKinesisEventNoRecords_returnsEmptyHeaders() {
        KinesisEvent event = new KinesisEvent();

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingKinesisEventFirstRecordHasNoDatadogAttributes_returnsEmptyHeaders() {
        KinesisEvent event = new KinesisEvent();
        event.setRecords(singletonList(new KinesisEventRecord()));

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingKinesisEventWithRecordWithDatadogAttributes_returnsHeaders() {
        KinesisEvent event = new KinesisEvent();
        KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();
        KinesisEvent.Record record = new KinesisEvent.Record();
        String payload = "{"
                + "    \"_datadog\": {"
                + "        \"x-datadog-trace-id\": \"123\","
                + "        \"x-datadog-parent-id\": \"456\","
                + "        \"x-datadog-sampling-priority\": \"4\""
                + "    },"
                + "    \"payload\": {"
                + "        \"foo\": \"bar\""
                + "    }"
                + "}";
        record.setData(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
        kinesisEventRecord.setKinesis(record);
        event.setRecords(singletonList(kinesisEventRecord));

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("x-datadog-trace-id", "123");
        expectedHeaders.put("x-datadog-parent-id", "456");
        expectedHeaders.put("x-datadog-sampling-priority", "4");

        Assert.assertEquals(expectedHeaders, headers);
    }

    @Test
    public void testParsingKinesisEventWithRecordWithoutTraceID_returnsEmptyHeaders() {
        KinesisEvent event = new KinesisEvent();
        KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();
        KinesisEvent.Record record = new KinesisEvent.Record();
        String payload = "{"
                + "    \"_datadog\": {"
                + "        \"x-datadog-parent-id\": \"456\","
                + "        \"x-datadog-sampling-priority\": \"2\""
                + "    },"
                + "    \"payload\": {"
                + "        \"foo\": \"bar\""
                + "    }"
                + "}";
        record.setData(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
        kinesisEventRecord.setKinesis(record);
        event.setRecords(singletonList(kinesisEventRecord));

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }

    @Test
    public void testParsingKinesisEventWithRecordWithoutSamplingPriority_returnsHeadersWithDefaultSamplingPriority() {
        KinesisEvent event = new KinesisEvent();
        KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();
        KinesisEvent.Record record = new KinesisEvent.Record();
        String payload = "{"
                + "    \"_datadog\": {"
                + "        \"x-datadog-trace-id\": \"123\","
                + "        \"x-datadog-parent-id\": \"456\""
                + "    },"
                + "    \"payload\": {"
                + "        \"foo\": \"bar\""
                + "    }"
                + "}";
        record.setData(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
        kinesisEventRecord.setKinesis(record);
        event.setRecords(singletonList(kinesisEventRecord));

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("x-datadog-trace-id", "123");
        expectedHeaders.put("x-datadog-parent-id", "456");
        expectedHeaders.put("x-datadog-sampling-priority", "2");

        Assert.assertEquals(expectedHeaders, headers);
    }

    @Test
    public void testParsingKinesisEventWithRecordWithMalformedJson_ReturnsEmptyHeadsers() {
        KinesisEvent event = new KinesisEvent();
        KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();
        KinesisEvent.Record record = new KinesisEvent.Record();
        String payload = "NOT A JSON";
        record.setData(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
        kinesisEventRecord.setKinesis(record);
        event.setRecords(singletonList(kinesisEventRecord));

        Map<String, String> headers = new KinesisHeaderable(event).getHeaders();

        assertTrue(headers.isEmpty());
    }
}
