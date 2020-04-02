package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.HashMap;
import java.util.Map;

class EnhancedMetric {
    public static Map<String, Object> makeTagsFromContext(Context ctx) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (ctx != null) {
            String[] arnParts = ctx.getInvokedFunctionArn().split(":");
            String region = "";
            String accountId = "";
            if (arnParts.length > 3) region = arnParts[3];
            if (arnParts.length > 4) accountId = arnParts[4];
            m.put("functionname", ctx.getFunctionName());
            m.put("region", region);
            m.put("account_id", accountId);
            m.put("memorysize", ctx.getMemoryLimitInMB());
            m.put("cold_start", ColdStart.getColdStart(ctx));
        } else {
            DDLogger.getLoggerImpl().debug("Unable to enhance metrics: context was null.");
        }
        String runtime = "java" + System.getProperty("java.version");
        m.put("runtime", runtime);
        return m;
    }
}
