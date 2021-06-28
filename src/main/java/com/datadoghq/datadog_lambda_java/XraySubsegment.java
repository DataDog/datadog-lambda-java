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
        this.setMetadata(m);
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public metadata_cl getMetadata() {
        return metadata;
    }

    public void setMetadata(metadata_cl metadata) {
        this.metadata = metadata;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public static class XraySubsegmentBuilder {
        private XraySubsegment xrs;

        public XraySubsegmentBuilder() {
            this.xrs = new XraySubsegment();
        }

        public XraySubsegmentBuilder startTime(Double startTime) {
            this.xrs.setStartTime(startTime);
            return this;
        }

        public XraySubsegmentBuilder endTime(Double endTime) {
            this.xrs.setEndTime(endTime);
            return this;
        }

        public XraySubsegmentBuilder traceId(String traceId) {
            this.xrs.setTraceId(traceId);
            return this;
        }

        public XraySubsegmentBuilder parentId(String parentId) {
            this.xrs.setParentId(parentId);
            return this;
        }

        public XraySubsegmentBuilder name(String name) {
            this.xrs.setName(name);
            return this;
        }

        public XraySubsegmentBuilder id(String id) {
            this.xrs.setId(id);
            return this;
        }

        public XraySubsegmentBuilder type(String type) {
            this.xrs.setType(type);
            return this;
        }

        public XraySubsegmentBuilder ddTraceId(String traceId){
            this.xrs.getMetadata().datadog.trace.setTraceId(traceId);
            return this;
        }

        public XraySubsegmentBuilder ddSamplingPriority (String samplingPriority){
            this.xrs.getMetadata().datadog.trace.setSamplingPriority(samplingPriority);
            return this;
        }

        public XraySubsegmentBuilder ddParentId (String parentId){
            this.xrs.getMetadata().datadog.trace.setParentId(parentId);
            return this;
        }


        public XraySubsegment build() {
            return this.xrs;
        }
    }

    @SerializedName("start_time")
    private Double startTime;

    @SerializedName("metadata")
    private metadata_cl metadata;

    @SerializedName("trace_id")
    private String traceId;

    @SerializedName("parent_id")
    private String parentId;

    private String name;

    @SerializedName("end_time")
    private Double endTime;

    private String id;

    private String type;
}

class metadata_datadog_trace {
    @SerializedName("trace-id")
    private String traceId;

    @SerializedName("sampling-priority")
    private String samplingPriority;

    @SerializedName("parent-id")
    private String parentId;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSamplingPriority() {
        return samplingPriority;
    }

    public void setSamplingPriority(String samplingPriority) {
        this.samplingPriority = samplingPriority;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}

class metadata_datadog {
    public metadata_datadog_trace trace;
}

class metadata_cl {
    public metadata_datadog datadog;
}