package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

class EnhancedMetric {
    @NotNull
    public static Map<String, Object> makeTagsFromContext(@NotNull Context ctx) {
        String[] arnParts = ctx.getInvokedFunctionArn().split(":");
        String region = "";
        String accountId = "";
        if (arnParts.length > 3) region = arnParts[3];
        if (arnParts.length > 4) accountId = arnParts[4];
        String runtime = "Java " + System.getProperty("java.version");

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("functionname", ctx.getFunctionName());
        m.put("region", region);
        m.put("account_id", accountId);
        m.put("memorysize", ctx.getMemoryLimitInMB());
        m.put("cold_start", ColdStart.getColdStart(ctx));
        m.put("runtime", runtime);
        return m;
    }
}
