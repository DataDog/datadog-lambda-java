package com.datadoghq.datadog_lambda_java;

import com.google.gson.Gson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

class DDLogger {
    public enum Level {
        DEBUG,
        ERROR
    }

    private static Level global_level;
    private transient Level local_level;

    @NotNull
    @Contract(" -> new")
    public static DDLogger getLoggerImpl(){

        if (global_level != null) return new DDLogger();

        String env_level = System.getenv("DD_LOG_LEVEL");
        if (env_level == null) env_level = Level.ERROR.toString();

        if (env_level.equalsIgnoreCase(Level.DEBUG.toString())){
            global_level = Level.DEBUG;
        }

        return new DDLogger();
    }

    private DDLogger(){
        this.local_level = global_level;
    }

    public void debug(String logMessage, Object ... args){
        if (this.local_level == Level.DEBUG){
            doLog(Level.DEBUG, logMessage, args);
        }
    }

    public void error(String logMessage, Object... args){
        doLog(Level.ERROR, logMessage, args);
    }

    private void doLog(Level l, String logMessage, Object[] args){
        StringBuilder argsSB = new StringBuilder("datadog: ");
        argsSB.append(logMessage);
        if (args != null) {
            for (Object a : args) {
                argsSB.append(" ");
                argsSB.append(a);
            }
        }

        Map<String, String> structuredLog  = new HashMap<String, String>();
        structuredLog.put("level", l.toString());
        structuredLog.put("message", argsSB.toString());

        Gson g = new Gson();

        System.out.println(g.toJson(structuredLog));

    }

    public void setLevel(Level l){
        this.local_level = l;
    }
}
