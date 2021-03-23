package com.datadoghq.datadog_lambda_java;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TracingTest {

    @Test
    public void makeOutboundHttpTraceHeaders() {
        DDTraceContext cxt = new DDTraceContext();
        XRayTraceContext xrt = new XRayTraceContext();

        cxt.samplingPriority = "2";
        cxt.parentID = "32342354323445";
        cxt.traceID = "12344567890";

        xrt.parentId = "53995c3f42cd8ad8";
        xrt.traceId = "1-5e41a79d-e6a0db584029dba86a594b7e";

        Tracing t = new Tracing();
        t.cxt = cxt;
        t.xrt = xrt;

        Map<String, String> headers = t.makeOutboundHttpTraceHeaders();

        Assert.assertEquals(3, headers.size());
        Assert.assertEquals("2", headers.get(cxt.ddSamplingKey));
        Assert.assertEquals("12344567890", headers.get(cxt.ddTraceKey));
        Assert.assertEquals("6023947403358210776", headers.get(cxt.ddParentKey));
    }

    @Test
    public void makeOutboundHttpTraceHeaders_no_parent() {
        DDTraceContext cxt = new DDTraceContext();
        XRayTraceContext xrt = new XRayTraceContext();

        cxt.samplingPriority = "2";
        cxt.parentID = "32342354323445";
        cxt.traceID = "12344567890";

        xrt.parentId = "";
        xrt.traceId = "1-5e41a79d-e6a0db584029dba86a594b7e";

        Tracing t = new Tracing();
        t.cxt = cxt;
        t.xrt = xrt;

        Map<String, String> headers = t.makeOutboundHttpTraceHeaders();

        Assert.assertEquals(0, headers.size());
    }

    @Test
    public void makeOutboundJson() {
        DDTraceContext cxt = new DDTraceContext();
        XRayTraceContext xrt = new XRayTraceContext();

        cxt.samplingPriority = "2";
        cxt.parentID = "32342354323445";
        cxt.traceID = "12344567890";

        xrt.parentId = "53995c3f42cd8ad8";
        xrt.traceId = "1-5e41a79d-e6a0db584029dba86a594b7e";

        Tracing t = new Tracing();
        t.cxt = cxt;
        t.xrt = xrt;

        String json = t.makeOutboundJson();

        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Gson g = new Gson();
        Map<String, String> tracingInfo = g.fromJson(json, type);

        Map<String, String> expectedTracingInfo = new HashMap<>();
        expectedTracingInfo.put(cxt.ddTraceKey, cxt.traceID);
        expectedTracingInfo.put(cxt.ddParentKey, "6023947403358210776");
        expectedTracingInfo.put(cxt.ddSamplingKey, cxt.samplingPriority);

        Assert.assertEquals(expectedTracingInfo, tracingInfo);
    }
}
