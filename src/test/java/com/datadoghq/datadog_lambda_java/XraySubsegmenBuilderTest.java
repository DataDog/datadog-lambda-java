package com.datadoghq.datadog_lambda_java;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class XraySubsegmenBuilderTest {

    @Test
    public void XraySubsegmentBuilderTest(){
        XraySubsegment.XraySubsegmentBuilder xrb = new XraySubsegment.XraySubsegmentBuilder();

        //{
        //  "start_time": 1500000000,
        //  "metadata": {
        //    "datadog": {
        //      "trace": {
        //        "trace-id": "abcdef",
        //        "sampling-priority": "1",
        //        "parent-id": "ghijk"
        //      }
        //    }
        //  },
        //  "trace_id": "1-5e41b3ba-9b515c884a780c0c63b74010",
        //  "parent_id": "30652c287aaff114",
        //  "name": "datadog-metadata",
        //  "end_time": 1500000001,
        //  "id": "30652c287aaff114",
        //  "type": "subsegment"
        //}

        XraySubsegment xrs = xrb.startTime(1500000000d)
                .traceId("1-5e41b3ba-9b515c884a780c0c63b74010")
                .parentId("30652c287aaff114")
                .name("datadog-metadata")
                .endTime(1500000001d)
                .id( "30652c287aaff114")
                .type("subsegment")
                .ddTraceId("abcdef")
                .ddSamplingPriority("1")
                .ddParentId("ghijk")
                .build();

        Assert.assertEquals(xrs.startTime, Double.valueOf(1500000000));
        Assert.assertEquals(xrs.endTime, Double.valueOf(1500000001));
        Assert.assertEquals(xrs.traceId, "1-5e41b3ba-9b515c884a780c0c63b74010");
        Assert.assertEquals(xrs.parentId, "30652c287aaff114");
        Assert.assertEquals(xrs.name, "datadog-metadata");
        Assert.assertEquals(xrs.id, "30652c287aaff114");
        Assert.assertEquals(xrs.type, "subsegment");
        Assert.assertEquals(xrs.metadata.datadog.trace.traceId, "abcdef");
        Assert.assertEquals(xrs.metadata.datadog.trace.samplingPriority, "1");
        Assert.assertEquals(xrs.metadata.datadog.trace.parentId, "ghijk");
    }

}