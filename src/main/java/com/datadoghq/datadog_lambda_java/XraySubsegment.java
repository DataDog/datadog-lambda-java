package com.datadoghq.datadog_lambda_java;


import com.google.gson.annotations.SerializedName;

class XraySubsegment {
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

    private XraySubsegment() {
        //Initialize inner metadata structure
        metadata_datadog_trace mdt = new metadata_datadog_trace();
        metadata_datadog md = new metadata_datadog();
        metadata_cl m = new metadata_cl();

        md.trace = mdt;
        m.datadog = md;
        this.metadata = m;
    }


    public static class XraySubsegmentBuilder {
        private XraySubsegment xrs;

        public XraySubsegmentBuilder() {
            this.xrs = new XraySubsegment();
        }

        public XraySubsegmentBuilder startTime(Double startTime) {
            this.xrs.startTime = startTime;
            return this;
        }

        public XraySubsegmentBuilder endTime(Double endTime) {
            this.xrs.endTime = endTime;
            return this;
        }

        public XraySubsegmentBuilder traceId(String traceId) {
            this.xrs.traceId = traceId;
            return this;
        }

        public XraySubsegmentBuilder parentId(String parentId) {
            this.xrs.parentId = parentId;
            return this;
        }

        public XraySubsegmentBuilder name(String name) {
            this.xrs.name = name;
            return this;
        }

        public XraySubsegmentBuilder id(String id) {
            this.xrs.id = id;
            return this;
        }

        public XraySubsegmentBuilder type(String type) {
            this.xrs.type = type;
            return this;
        }

        public XraySubsegmentBuilder ddTraceId(String traceId){
            this.xrs.metadata.datadog.trace.traceId = traceId;
            return this;
        }

        public XraySubsegmentBuilder ddSamplingPriority (String samplingPriority){
            this.xrs.metadata.datadog.trace.samplingPriority = samplingPriority;
            return this;
        }

        public XraySubsegmentBuilder ddParentId (String parentId){
            this.xrs.metadata.datadog.trace.parentId = parentId;
            return this;
        }


        public XraySubsegment build() {
            return this.xrs;
        }
    }

    @SerializedName("start_time")
    public Double startTime;

    @SerializedName("metadata")
    public metadata_cl metadata;

    @SerializedName("trace_id")
    public String traceId;

    @SerializedName("parent_id")
    public String parentId;

    public String name;

    @SerializedName("end_time")
    public Double endTime;

    public String id;

    public String type;
}

class metadata_datadog_trace {
    @SerializedName("trace-id")
    public String traceId;

    @SerializedName("sampling-priority")
    public String samplingPriority;

    @SerializedName("parent-id")
    public String parentId;
}

class metadata_datadog {
    public metadata_datadog_trace trace;
}

class metadata_cl {
    public metadata_datadog datadog;
}