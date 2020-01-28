package com.datadoghq.datadog_lambda_layer_java;


import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

// '{"e":1215508260,"m":"m1","t":["dd_lambda_layer:datadog-ruby25","t.a:val","t.b:v2"],"v":100}'
public class DDMetric {
    private String name;
    private double value;
    private Map<String, String> tags;
    private Date time;

    public DDMetric(String name, double value){
        this.name = name;
        this.value = value;
        this.time = new Date();
    }

    public DDMetric(String name, double value, Map<String, String> tags){
        this.name = name;
        this.value = value;
        this.tags = tags;
        this.time = new Date();
    }

    public String toJson(){
        JSONObject jo = new JSONObject()
                .put("m", this.name)
                .put("v", this.value);
        return jo.toString();
    }


}
