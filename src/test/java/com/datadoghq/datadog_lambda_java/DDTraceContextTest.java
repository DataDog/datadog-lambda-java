package com.datadoghq.datadog_lambda_java;

import org.junit.Test;


import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DDTraceContextTest {

    @Test(expected = Exception.class)
    public void testDDTraceContextConstructor_throwsOnNullHeaders() throws Exception {
        DDTraceContext ddt = new DDTraceContext(null);
    }

    @Test(expected = Exception.class)
    public void testDDTraceContextConstructor_throwsOnHeadersMissingRequiredValues() throws Exception{
        Map<String,String> dummyHeaders  = new HashMap<String, String>();
        dummyHeaders.put("foo", "bar");
        dummyHeaders.put("baz","luhrmann");
        DDTraceContext ddt = new DDTraceContext(dummyHeaders);
    }

    @Test
    public void testDDTraceContextConstructor_constructsDDTraceContext(){
        Map<String, String> dummyHeaders = new HashMap<String, String>();
        String ddTraceKey = "x-datadog-trace-id";
        String ddParentKey = "x-datadog-parent-id";
        String ddSamplingKey = "x-datadog-sampling-priority";

        dummyHeaders.put(ddTraceKey, "foo");
        dummyHeaders.put(ddParentKey, "bar");
        dummyHeaders.put(ddSamplingKey, "baz");

        DDTraceContext ddt = null;
        try {
            ddt = new DDTraceContext(dummyHeaders);
        } catch (Exception e) {
            assertNull("Should not reach this exception", e);
        }

        assertEquals("foo", ddt.getTraceID());
        assertEquals("bar", ddt.getParentID());
        assertEquals("baz", ddt.getSamplingPriority());
    }

}