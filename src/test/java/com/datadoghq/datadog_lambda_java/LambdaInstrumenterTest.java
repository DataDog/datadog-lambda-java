package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LambdaInstrumenterTest {
    @Before
    public void setUp() throws Exception {
        ColdStart.resetColdStart();
    }

    private boolean JSONArrayContains(JSONArray j, String needle){
        for (Object jo: j){
            if(jo.toString().equals(needle)) {
                return true;
            }
        }
        return false;
    }

    @Test public void TestLambdaInstrumentor(){
        ColdStart.resetColdStart();
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);
        LambdaInstrumenter li = new LambdaInstrumenter(mc);

        Assert.assertNotNull(omw.CM);

        JSONObject writtenMetric = new JSONObject(omw.CM.toJson());
        Assert.assertEquals( "aws.lambda.enhanced.invocations", writtenMetric.get("m").toString());
        JSONArray jsonTags = (JSONArray) writtenMetric.get("t");
        Assert.assertTrue(JSONArrayContains(jsonTags, "cold_start:true"));

        EnhancedMetricTest.MockContext mc2 = new EnhancedMetricTest.MockContext();
        LambdaInstrumenter li2 = new LambdaInstrumenter(mc2);

        JSONObject writtenMetric2 = new JSONObject(omw.CM.toJson());
        JSONArray jsonTags2 = (JSONArray) writtenMetric2.get("t");
        Assert.assertTrue(JSONArrayContains(jsonTags2, "cold_start:false"));
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
        Assert.assertEquals("aws.lambda.enhanced.errors", jsonObject.getString("m"));
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

    @Test public void TestLambdaInstrumentorCountsColdStartErrors(){
        ColdStart.resetColdStart();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        Context mc1 = new EnhancedMetricTest.MockContext();
        LambdaInstrumenter li =new LambdaInstrumenter(mc1);
        li.recordError(mc1);

        JSONObject thisMetric = new JSONObject(omw.CM.toJson());
        Assert.assertEquals("aws.lambda.enhanced.errors", thisMetric.get("m").toString());

        JSONArray theseTags = (JSONArray) thisMetric.get("t");
        Assert.assertTrue(JSONArrayContains(theseTags, "cold_start:true"));
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