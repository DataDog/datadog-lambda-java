package com.datadoghq.datadog_lambda_java;

import org.junit.Assert;
import org.junit.Test;

public class XRayTraceContextTest {

    @Test
    public void getAPMParentFromXray_happy_path() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setParentId("0b11cc4230d3e09e");
        Assert.assertEquals("797643193680388254", xrt.getAPMParentID());

        xrt.setParentId("53995c3f42cd8ad8");
        Assert.assertEquals("6023947403358210776", xrt.getAPMParentID());

        xrt.setParentId("1000000000000000");
        Assert.assertEquals("1152921504606846976", xrt.getAPMParentID());

        xrt.setParentId("ffffffffffffffff");
        Assert.assertEquals("18446744073709551615", xrt.getAPMParentID());
    }


    @Test
    public void getAPMParentFromXray_bad_characters() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setParentId(";79014b90ce44db5e0;875");
        Assert.assertNull(xrt.getAPMParentID());
    }

    @Test
    public void getAPMParentFromXray_too_short() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setParentId("875");
        Assert.assertNull(xrt.getAPMParentID());
    }

    @Test
    public void getAPMTraceID() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setTraceId("1-5ce31dc2-2c779014b90ce44db5e03875");
        String traceId = xrt.getAPMTraceID();
        Assert.assertEquals("4110911582297405557", traceId);
    }

    @Test
    public void getAPMTraceID_too_short() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setTraceId("1-5ce31dc2-5e03875");
        String traceId = xrt.getAPMTraceID();
        Assert.assertEquals("", traceId);
    }

    @Test
    public void getAPMTraceID_invalid_format() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setTraceId("1-2c779014b90ce44db5e03875");
        String traceId = xrt.getAPMTraceID();
        Assert.assertEquals("", traceId);
    }

    @Test
    public void getAPMTraceID_incorrect_characters() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setTraceId("1-5ce31dc2-c779014b90ce44db5e03875;");
        String traceId = xrt.getAPMTraceID();
        Assert.assertEquals("", traceId);
    }
}