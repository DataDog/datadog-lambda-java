package com.datadoghq.datadog_lambda_layer_java;


import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
        //First we need to turn the tags into an array of colon-delimited strings
        ArrayList<String> tagsList = new java.util.ArrayList<String>();
        if (this.tags != null) {
            this.tags.forEach((k, v) -> tagsList.add(String.format("%s:%s", k, v.toString())));
        }

        long unixTime  = this.time.getTime() / 1000; // To Unix seconds instead of millis
        JSONObject jo = new JSONObject()
                .put("e", unixTime)
                .put("m", this.name)
                .put("v", this.value)
                .put("t", tagsList);
        return jo.toString();
    }

    /**
     * Write writes the CustomMetric to Datadog
     */
    public void write(){
        MetricWriter writer = MetricWriter.getMetricWriterImpl();
        writer.write(this);
    }
}
