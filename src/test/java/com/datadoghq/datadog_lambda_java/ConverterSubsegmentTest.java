package com.datadoghq.datadog_lambda_java;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ConverterSubsegmentTest {

    @Test
    public void toJSON() throws Exception{
        Map<String, String> headers  = new HashMap<String, String>();
        headers.put("x-datadog-trace-id", "abcdef");
        headers.put("x-datadog-parent-id", "ghijk");
        headers.put("x-datadog-sampling-priority", "1");

        DDTraceContext ddt = new DDTraceContext(headers);
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setParentId("30652c287aaff114");
        xrt.setTraceId("1-5e41b3ba-9b515c884a780c0c63b74010");
        xrt.setTraceIdHeader("foo bar baz");
        ConverterSubsegment es = new ConverterSubsegment(ddt, xrt);
        es.setStart_time(1_500_000_000D);
        es.setEnd_time(1_500_000_001D);
        es.setId("30652c287aaff114");

        String segJSON = es.toJSONString();
        String expectedStr = "{\"start_time\":1.5E9,\"metadata\":{\"datadog\":{\"trace\":{\"trace-id\":\"abcdef\",\"sampling-priority\":\"1\",\"parent-id\":\"ghijk\"}}},\"trace_id\":\"1-5e41b3ba-9b515c884a780c0c63b74010\",\"parent_id\":\"30652c287aaff114\",\"name\":\"datadog-metadata\",\"end_time\":1.500000001E9,\"id\":\"30652c287aaff114\",\"type\":\"subsegment\"}";
        Assert.assertEquals(expectedStr, segJSON);

    }
}