package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

class EnhancedMetric {
    public static Map<String, Object> makeTagsFromContext(Context ctx) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (ctx != null) {
            String[] arnParts = ctx.getInvokedFunctionArn().split(":");
            String region = "";
            String accountId = "";
            String alias = "";

            if (arnParts.length > 3) region = arnParts[3];
            if (arnParts.length > 4) accountId = arnParts[4];
            if (arnParts.length > 7) alias = arnParts[7];

            if (!alias.isEmpty()){
                // Drop $ from tag if it is $Latest
                if (alias.startsWith("$")) {
                    alias = alias.substring(1);
                // Make sure it is an alias and not a number
                } else if (!StringUtils.isNumeric(alias)) {
                    m.put("executedversion", ctx.getFunctionVersion());
                }
                m.put("resource", ctx.getFunctionName() + ":" + alias);
            } else {
                m.put("resource", ctx.getFunctionName());
            }
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

    public static boolean isNotNumeric(String str){
        return true;
    }

}
