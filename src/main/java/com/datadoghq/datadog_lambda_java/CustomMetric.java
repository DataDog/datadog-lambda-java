package com.datadoghq.datadog_lambda_java;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * All the information for a custom Datadog distribution metric.
 */
public class CustomMetric {
    private String name;
    private double value;
    private Map<String, Object> tags;
    private Date time;

    /**
     * Create a custom distribution metric
     * @param name The name assigned to the metric
     * @param value The value of the metric
     * @param tags A map of tags (if any) that you want to assign to the metric
     */
    public CustomMetric(String name, double value, Map<String, Object> tags){
        this(name, value, tags, new Date());
    }

    /**
     * Create a custom distribution metric with custom a custom time
     * @param name The name assigned to the metric
     * @param value The value of the metric
     * @param tags A map of tags (if any) that you want to assign to the metric
     * @param time The time that you want to give to the metric
     */
    public CustomMetric(String name, double value, Map<String, Object> tags, Date time){
        this.name = name;
        this.value = value;
        this.tags = tags;
        this.time = time;
    }

    /**
     * Create a JSON string representing the distribution metric
     * @return the Metric's JSON representation
     */
    public String toJson(){
        PersistedCustomMetric pcm = new PersistedCustomMetric(this.name, this.value, this.tags, this.time);
        return pcm.toJsonString();
    }

    /**
     * Write writes the CustomMetric to Datadog
     */
    public void write(){
        MetricWriter writer = MetricWriter.getMetricWriterImpl();
        writer.write(this);
    }
}

class PersistedCustomMetric{
    public PersistedCustomMetric(String m, double v, Map<String, Object>t, Date e){
        this.metric = m;
        this.value = v;

        //First we need to turn the tags into an array of colon-delimited strings
        ArrayList<String> tagsList = new java.util.ArrayList<String>();
        if (t != null) {
            t.forEach((k, val) -> tagsList.add(String.format("%s:%s", k, val.toString())));
        }
        this.tags = tagsList;
        long unixTime  = e.getTime() / 1000; // To Unix seconds instead of millis
        this.eventTime = unixTime;
    }

    @SerializedName("m")
    public String metric;

    @SerializedName("v")
    public Double value;

    @SerializedName("t")
    public ArrayList<String> tags;

    @SerializedName("e")
    public long eventTime;

    public String toJsonString(){
        Gson g  =  new Gson();
        return g.toJson(this);
    }
}
