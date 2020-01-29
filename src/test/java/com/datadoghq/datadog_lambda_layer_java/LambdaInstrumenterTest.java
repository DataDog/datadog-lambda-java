package com.datadoghq.datadog_lambda_layer_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class LambdaInstrumenterTest {
    @Before
    public void setUp() throws Exception {
        ColdStart.resetColdStart();
    }

    @Test public void TestLambdaInstrumentor(){
        ColdStart.resetColdStart();
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);
        LambdaInstrumenter li = new LambdaInstrumenter(mc);

        Assert.assertNotNull(omw.CM);

        JSONObject writtenMetric = new JSONObject(omw.CM.toJson());
        Assert.assertEquals( "aws.lambda.enhanced.invocation", writtenMetric.get("m").toString());
        JSONObject jsonTags = (JSONObject) writtenMetric.get("t");
        Assert.assertTrue((boolean) jsonTags.get("cold_start"));

        LambdaInstrumenter li2 = new LambdaInstrumenter(mc);

        JSONObject writtenMetric2 = new JSONObject(omw.CM.toJson());
        JSONObject jsonTags2 = (JSONObject) writtenMetric2.get("t");
        Assert.assertFalse((boolean) jsonTags2.get("cold_start"));
    }

    @Test public void TestLambdaInstrumentorWithNullContext(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        LambdaInstrumenter li =new LambdaInstrumenter(null);
        Assert.assertNotNull(omw.CM);
    }
    @Test public void TestLambdaInstrumentorFlush(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        LambdaInstrumenter li =new LambdaInstrumenter(null);
        Assert.assertNotNull(omw.CM);
        li.flush();
        Assert.assertNull(omw.CM);
    }

    @Test public void TestLambdaInstrumentorError(){
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        LambdaInstrumenter li =new LambdaInstrumenter(mc);
        li.recordError(mc);

        JSONObject jsonObject = new JSONObject(omw.CM.toJson());
        Assert.assertEquals("aws.lambda.enhanced.error", jsonObject.getString("m"));
    }

    @Test public void TestLambdaInstrumentorCustomMetric(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        LambdaInstrumenter li =new LambdaInstrumenter(null);
        li.flush();

        li.recordCustomMetric("my_custom_metric", 37.1, null);
        Assert.assertNotNull(omw.CM);

        JSONObject jsonObject = new JSONObject(omw.CM.toJson());
        Assert.assertEquals("my_custom_metric", jsonObject.get("m"));
        Assert.assertEquals(37.1, jsonObject.get("v"));
    }

    @After
    public void tearDown() throws Exception {
        ColdStart.resetColdStart();
    }
}


class ObjectMetricWriter extends MetricWriter{
    public CustomMetric CM;
    @Override
    public void write(CustomMetric cm) {
        this.CM = cm;
    }

    @Override
    public void flush() {
        this.CM = null;
    }
}