package com.datadoghq.datadog_lambda_java;

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(xrs.getStartTime(), Double.valueOf(1500000000));
        Assert.assertEquals(xrs.getEndTime(), Double.valueOf(1500000001));
        Assert.assertEquals(xrs.getTraceId(), "1-5e41b3ba-9b515c884a780c0c63b74010");
        Assert.assertEquals(xrs.getParentId(), "30652c287aaff114");
        Assert.assertEquals(xrs.getName(), "datadog-metadata");
        Assert.assertEquals(xrs.getId(), "30652c287aaff114");
        Assert.assertEquals(xrs.getType(), "subsegment");
        Assert.assertEquals(xrs.getMetadata().datadog.trace.traceId, "abcdef");
        Assert.assertEquals(xrs.getMetadata().datadog.trace.samplingPriority, "1");
        Assert.assertEquals(xrs.getMetadata().datadog.trace.parentId, "ghijk");
    }

}