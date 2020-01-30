package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ColdStartTest {
    @Test public void testColdStart(){
        Context firstContext = new EnhancedMetricTest.MockContext();
        Context subsequentContext = new EnhancedMetricTest.MockContext();

        Assert.assertTrue(ColdStart.getColdStart(firstContext));
        Assert.assertFalse(ColdStart.getColdStart(subsequentContext));
        ColdStart.resetColdStart();
        Assert.assertTrue(ColdStart.getColdStart(subsequentContext));
    }

    @Before
    public void setUp() throws Exception {
        ColdStart.resetColdStart();
    }

    @After
    public void tearDown() throws Exception {
        ColdStart.resetColdStart();
    }
}