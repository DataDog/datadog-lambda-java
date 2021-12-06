package com.datadoghq.datadog_lambda_java;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class Extension {

    private final static String AGENT_URL = "http://127.0.0.1:8124";
    private final static String HELLO_PATH = "/lambda/hello";
    private final static String FLUSH_PATH = "/lambda/flush";
    private final static String EXTENSION_PATH = "/opt/extensions/datadog-agent";

    protected static boolean setup() {
        boolean shouldUseExtension = false;
        if(isExtensionRunning(EXTENSION_PATH)) {
            DDLogger.getLoggerImpl().debug("Extension has been detected");
            if(hitHelloRoute(AGENT_URL, HELLO_PATH)) {
                shouldUseExtension = true;
            } else {
                DDLogger.getLoggerImpl().debug("Could not call the hello route");
            }
        }
        return shouldUseExtension;
    }

    protected static void flush() {
        if(!hitFlushRoute(AGENT_URL, FLUSH_PATH)) {
            DDLogger.getLoggerImpl().debug("Could not call the flush route");
        }
    }

    protected static boolean isExtensionRunning(final String extensionPath) {
        File f = new File(extensionPath);
        return (f.exists() && !f.isDirectory());
    }

    protected static boolean hitHelloRoute(final String agentUrl, final String helloPath) {
        try {
            URL url = new URL(agentUrl + helloPath);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            return http.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    protected static boolean hitFlushRoute(final String agentUrl, final String flushPath) {
        try {
            URL url = new URL(agentUrl + flushPath);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("POST");
            return http.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

}