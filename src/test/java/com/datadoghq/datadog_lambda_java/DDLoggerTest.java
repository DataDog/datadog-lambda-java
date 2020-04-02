package com.datadoghq.datadog_lambda_java;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


public class DDLoggerTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Test
    public void debug() {
        DDLogger ddlerror =  DDLogger.getLoggerImpl();
        ddlerror.debug("shouldn't show up");
        Assert.assertEquals("", outContent.toString());
        outContent.reset();

        DDLogger ddldebug = DDLogger.getLoggerImpl();
        ddldebug.setLevel(DDLogger.level.DEBUG);
        ddldebug.debug("foo");
        Assert.assertEquals("{\"level\":\"DEBUG\",\"message\":\"datadog: foo\"}\n", outContent.toString());

        outContent.reset();
        ddlerror.debug("shouldn't show up");
        Assert.assertEquals("", outContent.toString());
        outContent.reset();
    }

    @Test
    public void error() {
        outContent.reset();
        DDLogger ddLogger = DDLogger.getLoggerImpl();
        ddLogger.setLevel(DDLogger.level.DEBUG);

        ddLogger.error("bad news!");
        Assert.assertEquals("{\"level\":\"ERROR\",\"message\":\"datadog: bad news!\"}\n", outContent.toString());
        outContent.reset();

        ddLogger.setLevel(DDLogger.level.ERROR);
        ddLogger.error("more bad news!");
        Assert.assertEquals("{\"level\":\"ERROR\",\"message\":\"datadog: more bad news!\"}\n", outContent.toString());
        outContent.reset();
    }

    @Before
    public void setUp() throws Exception {
        System.setOut(new PrintStream(outContent));

    }

    @After
    public void tearDown() throws Exception {
        System.setOut(originalOut);
    }
}