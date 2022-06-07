package com.datadoghq.datadog_lambda_java;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

import java.util.stream.Collectors;

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

    private static StatsDClient client;
    private static ExtensionMetricWriter emw;

    public static ExtensionMetricWriter GetInstance(){
        if (null == emw){
            emw = new ExtensionMetricWriter();
        }
        return emw;
    }

    private ExtensionMetricWriter() {
        if (null == client) {
            try {
                client = new NonBlockingStatsDClientBuilder()
                        .prefix("")
                        .hostname("127.0.0.1")
                        .port(8125)
                        .enableTelemetry(false)
                        .telemetryFlushInterval(0)
                        .build();
            } catch (Exception e) {
                DDLogger.getLoggerImpl().error("Could not create StatsDClient " + e.getMessage());
                client = null;
            }
        }
    }



    @Override
    public void write(CustomMetric cm){
        if(null != client) {
            String tags = "";
            if (cm.getTags() != null) {
                tags = cm
                    .getTags()
                    .entrySet()
                    .stream()
                    .map(
                        entry -> entry.getKey().toLowerCase() + ":" + entry.getValue().toString().toLowerCase()
                    ).collect(Collectors.joining(","));
            }
            client.distribution(cm.getName(), cm.getValue(), tags);
        } else {
            DDLogger.getLoggerImpl().error("Could not write the metric because the client is null");
        }
    }

    @Override
    public void flush(){}
}

