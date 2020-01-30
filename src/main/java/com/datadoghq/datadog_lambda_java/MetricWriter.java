package com.datadoghq.datadog_lambda_java;


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

