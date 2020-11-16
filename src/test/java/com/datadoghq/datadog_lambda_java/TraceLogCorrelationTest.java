package com.datadoghq.datadog_lambda_java;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.PatternLayout;


public class TraceLogCorrelationTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @Test
    public void TestWithLog4J(){
        Logger logger = Logger.getLogger(TraceLogCorrelationTest.class.getName());
        PatternLayout layout = new PatternLayout("%d{ISO8601} %X{dd.trace_context} [%t] %-5p %c %x - %m%n");
        logger.addAppender(new ConsoleAppender((layout)));

        logger.info("Test log message");

        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();

        //try to grab the contents of StdErr
        logger.addAppender(new WriterAppender(layout, outContent));
        DDLambda ddl = new DDLambda(mc, "Root=1-5fa98677-5dda00b12ec8b1c31f61aa82;Parent=b481b4a6ef3a296c;Sampled=1");
        outContent.reset();
        logger.info("Test log message 2");
        logger.removeAllAppenders();

        String logOutput  = outContent.toString();
        String[] logFields = logOutput.split(" ");
        Assert.assertEquals("[dd.trace_id=3371139772690049666", logFields[2]);
        Assert.assertEquals("dd.span_id=13006875827893840236]", logFields[3]);
    }

    @Test
    public void TestGetTraceLogCorrelationId(){
        EnhancedMetricTest.MockContext mc = new EnhancedMetricTest.MockContext();
        DDLambda ddl = new DDLambda(mc, "Root=1-5fa98677-5dda00b12ec8b1c31f61aa82;Parent=b481b4a6ef3a296c;Sampled=1");

        Map<String,String> traceInfo = ddl.getTraceContext();
        Assert.assertEquals("3371139772690049666", traceInfo.get("dd.trace_id"));
        Assert.assertEquals("13006875827893840236", traceInfo.get("dd.span_id"));
    }

}

