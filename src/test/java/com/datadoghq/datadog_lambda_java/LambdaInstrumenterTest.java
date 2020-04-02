package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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
        DDLambda li = new DDLambda(mc);

        Assert.assertNotNull(omw.CM);

        Gson g = new Gson();
        PersistedCustomMetric writtenMetric = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);

        Assert.assertEquals( "aws.lambda.enhanced.invocations", writtenMetric.metric.toString());
        Assert.assertTrue(writtenMetric.tags.contains("cold_start:true"));

        EnhancedMetricTest.MockContext mc2 = new EnhancedMetricTest.MockContext();
        DDLambda li2 = new DDLambda(mc2);

        PersistedCustomMetric writtenMetric2 = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertTrue(writtenMetric2.tags.contains("cold_start:false"));
    }

    @Test public void TestLambdaInstrumentorWithNullContext(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        DDLambda li =new DDLambda(null);
        Assert.assertNotNull(omw.CM);
    }

    @Test public void TestLambdaInstrumentorError(){
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        DDLambda li =new DDLambda(mc);
        li.error(mc);

        Gson g = new Gson();
        PersistedCustomMetric pcm = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertEquals("aws.lambda.enhanced.errors", pcm.metric);
    }

    @Test public void TestLambdaInstrumentorCustomMetric(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        DDLambda li =new DDLambda(null);

        li.metric("my_custom_metric", 37.1, null);
        Assert.assertNotNull(omw.CM);

        Gson g = new Gson();
        PersistedCustomMetric pcm = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertEquals("my_custom_metric", pcm.metric);
        Assert.assertEquals(Double.valueOf(37.1), pcm.value);
    }

    @Test public void TestLambdaInstrumentorCountsColdStartErrors(){
        ColdStart.resetColdStart();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        Context mc1 = new EnhancedMetricTest.MockContext();
        DDLambda li =new DDLambda(mc1);
        li.error(mc1);

        Gson g  = new Gson();
        PersistedCustomMetric pcm = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertEquals("aws.lambda.enhanced.errors", pcm.metric);

        Assert.assertTrue(pcm.tags.contains("cold_start:true"));
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