package com.datadoghq.datadog_lambda_layer_java;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ColdStartTest {
    @Test public void testColdStart(){
        Assert.assertTrue(ColdStart.getColdStart());
        Assert.assertFalse(ColdStart.getColdStart());
        ColdStart.resetColdStart();
        Assert.assertTrue(ColdStart.getColdStart());
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