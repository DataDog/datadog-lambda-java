package com.datadoghq.datadog_lambda_java;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

abstract class MetricWriter {
    private static MetricWriter IMPL;
    public static synchronized MetricWriter getMetricWriterImpl(){
        if (IMPL == null){
            //Potential to check for an env var and choose a different writer if we decide to support that
            IMPL = new StdoutMetricWriter();
        }
        return IMPL;
    }

    /**
     * Gives you the ability to set the metrics writer, for testing purposes
     * @param mw the new Metrics Write implementation
     */
    public static void setMetricWriter(MetricWriter mw){
        IMPL = mw;
    }

    public abstract void write(CustomMetric cm);
    public abstract void flush();
}

class StdoutMetricWriter extends MetricWriter{
    @Override
    public void write(CustomMetric cm){
       System.out.println(cm.toJson());
    }

    @Override
    public void flush(){}
}

class ExtensionMetricWriter extends MetricWriter{

    private StatsDClient client;

    public ExtensionMetricWriter() {
        try {
            this.client = new NonBlockingStatsDClientBuilder()
                    .prefix("")
                    .hostname("127.0.0.1")
                    .port(8125)
                    .enableTelemetry(false)
                    .telemetryFlushInterval(0)
                    .build();
        } catch (Exception e) {
            DDLogger.getLoggerImpl().error("Could not create StatsDClient " + e.getMessage());
            this.client = null;
        }
    }



    @Override
    public void write(CustomMetric cm){
        if(null != client) {
            StringBuilder tagsSb = new StringBuilder();
            if (cm.getTags() != null) {
                cm.getTags().forEach((k, val) ->
                        tagsSb.append(k.toLowerCase())
                                .append(":")
                                .append(val.toString().toLowerCase()));
            }
            client.distribution(cm.getName(), cm.getValue(), tagsSb.toString());
        } else {
            DDLogger.getLoggerImpl().error("Could not write the metric because the client is null");
        }
    }

    @Override
    public void flush(){}
}

