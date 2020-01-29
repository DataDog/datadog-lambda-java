package com.datadoghq.datadog_lambda_layer_java;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class EnhancedMetricTest {

    @Test
    public void makeTagsFromContext() {
        Map<String, Object> tags = EnhancedMetric.makeTagsFromContext(new MockContext());
        Assert.assertTrue(tags.containsKey("functionname"));
        Assert.assertTrue(tags.containsKey("region"));
        Assert.assertTrue(tags.containsKey("account_id"));
        Assert.assertTrue(tags.containsKey("memorysize"));
        Assert.assertTrue(tags.containsKey("cold_start"));
        Assert.assertTrue(tags.containsKey("runtime"));

        Assert.assertEquals("us-east-1", tags.get("region"));
        Assert.assertEquals("172597598159", tags.get("account_id"));
    }

    static class MockContext implements Context {

        @Override
        public String getAwsRequestId() {
            return "c224f41c-2384-4318-a7d5-879c2bbd4762";
        }

        @Override
        public String getLogGroupName() {
            return "/aws/lambda/lambda-sample-java-dev-helloJava11";
        }

        @Override
        public String getLogStreamName() {
            return "2020/01/29/[$LATEST]076e6e7068a64d38afa30a3e06a4aec3";
        }

        @Override
        public String getFunctionName() {
            return "lambda-sample-java-dev-helloJava11";
        }

        @Override
        public String getFunctionVersion() {
            return "$LATEST";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:us-east-1:172597598159:function:lambda-sample-java-dev-helloJava11";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 29910;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 128;
        }

        @Override
        public LambdaLogger getLogger() {
            return null;
        }
    }
}