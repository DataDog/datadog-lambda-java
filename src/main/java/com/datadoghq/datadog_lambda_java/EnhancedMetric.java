package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

class EnhancedMetric {
    private static final int EXPECTED_REGION_ARN_PART = 3;
    private static final int EXPECTED_ACCOUNT_ARN_PART = 4;
    private static final int EXPECTED_ALIAS_ARN_PART=7;

    public static Map<String, Object> makeTagsFromContext(Context ctx) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (ctx != null) {
            String[] arnParts = ctx.getInvokedFunctionArn().split(":");
            String region;
            String accountId;
            String alias;

            if (arnParts.length > EXPECTED_REGION_ARN_PART) {
                region = arnParts[EXPECTED_REGION_ARN_PART];
            } else {
                region = "";
            }
            if (arnParts.length > EXPECTED_ACCOUNT_ARN_PART) {
                accountId = arnParts[EXPECTED_ACCOUNT_ARN_PART];
            } else {
                accountId = "";
            }
            if (arnParts.length > EXPECTED_ALIAS_ARN_PART) {
                alias = arnParts[EXPECTED_ALIAS_ARN_PART];
            } else {
                alias = "";
            }

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
            m.put("datadog_lambda", BuildConfig.datadog_lambda_version);
        } else {
            DDLogger.getLoggerImpl().debug("Unable to enhance metrics: context was null.");
        }
        String runtime = "java" + System.getProperty("java.version");
        m.put("runtime", runtime);
        return m;
    }

}
