package com.datadoghq.datadog_lambda_java;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class XRayTraceContextTest {

    @Test
    public void getAPMParentFromXray() {
        XRayTraceContext xrt = new XRayTraceContext();
        xrt.setParent_id("0b11cc4230d3e09e");

        Assert.assertEquals("797643193680388254", xrt.getAPMParentID());
    }
}