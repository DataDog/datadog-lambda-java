package com.datadoghq.datadog_lambda_java;

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
}