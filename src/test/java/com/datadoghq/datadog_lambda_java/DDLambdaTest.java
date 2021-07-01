package com.datadoghq.datadog_lambda_java;

import java.util.Date;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


public class DDLambdaTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setUp() throws Exception {
        ColdStart.resetColdStart();
    }

    @Test public void TestDDLambda(){
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

    @Test public void TestDDLambdaWithNullContext(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        DDLambda li =new DDLambda(null);
        Assert.assertNotNull(omw.CM);
    }

    @Test public void TestDDLambdaError(){
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        DDLambda li =new DDLambda(mc);
        li.error(mc);

        Gson g = new Gson();
        PersistedCustomMetric pcm = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertEquals("aws.lambda.enhanced.errors", pcm.metric);
    }

    @Test public void TestDDLambdaCustomMetric(){
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

    @Test public void TestDDLambdaCustomMetricWithDate(){
        ObjectMetricWriter omw = new ObjectMetricWriter();
        MetricWriter.setMetricWriter(omw);

        Date date = new Date();
        date.setTime(1590420166419L);
        DDLambda li =new DDLambda(null);

        li.metric("my_custom_metric", 37.1, null, date);
        Assert.assertNotNull(omw.CM);

        Gson g = new Gson();
        PersistedCustomMetric pcm = g.fromJson(omw.CM.toJson(), PersistedCustomMetric.class);
        Assert.assertEquals("my_custom_metric", pcm.metric);
        Assert.assertEquals(Double.valueOf(37.1), pcm.value);
        Assert.assertEquals(Double.valueOf(37.1), pcm.value);
        Assert.assertEquals(1590420166, pcm.eventTime);
    }

    @Test public void TestDDLambdaCountsColdStartErrors(){
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

    @Test
    public void EnhancedMetricsDeactivatedWithEnvVar(){
        environmentVariables.set("DD_ENHANCED_METRICS", "false");
        Context mc1 = new EnhancedMetricTest.MockContext();
        DDLambda ddl = new DDLambda(mc1);
        boolean isEnhanced = ddl.checkEnhanced();
        Assert.assertFalse(isEnhanced);
        environmentVariables.clear("DD_ENHANCED_METRICS");
    }

    @Test
    public void EnhancedMetricsActivatedByDefault(){
        System.out.println(System.getenv("DD_ENHANCED_METRICS"));
        Context mc1 = new EnhancedMetricTest.MockContext();
        DDLambda ddl = new DDLambda(mc1);
        boolean isEnhanced = ddl.checkEnhanced();
        Assert.assertTrue(isEnhanced);
    }

    @Test
    public void EnhancedMetricsActivatedWithEnvVar(){
        environmentVariables.set("DD_ENHANCED_METRICS", "true");
        Context mc1 = new EnhancedMetricTest.MockContext();
        DDLambda ddl = new DDLambda(mc1);
        boolean isEnhanced = ddl.checkEnhanced();
        Assert.assertTrue(isEnhanced);
        environmentVariables.clear("DD_ENHANCED_METRICS");
    }

    @Test
    public void ArnsAreSanitizedCorrectly(){
        DDLambda ddl = new DDLambda(null);
        String sanitized = ddl.santitizeFunctionArn("arn:aws:lambda:us-west-1:601427279990:function:lambda-tracing-integration-java-2-dev-hello");
        Assert.assertEquals("arn:aws:lambda:us-west-1:601427279990:function:lambda-tracing-integration-java-2-dev-hello", sanitized);

        sanitized = ddl.santitizeFunctionArn("arn:aws:lambda:us-west-1:601427279990:function:lambda-tracing-integration-java-2-dev-hello:LATEST");
        Assert.assertEquals("arn:aws:lambda:us-west-1:601427279990:function:lambda-tracing-integration-java-2-dev-hello", sanitized);
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

