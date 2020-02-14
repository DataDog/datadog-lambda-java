package com.datadoghq.datadog_lambda_java;

import java.util.Map;

/**
 * Headerable is an interface that custom request objects must implement in order to benefit from Datadog tracing.
 * Headerable, when combined with the correct API Gateway Mapping Template (including the default application/JSON and
 * application/x-www-form-urlencoded templates) allow Lambda to write the HTTP Request Headers to your custom Java event.
 *
 */
public interface Headerable {
    public Map<String,String> getHeaders();
    public void setHeaders(Map<String,String> headers);
}
