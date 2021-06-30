package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.datadoghq.datadog_lambda_java.EnhancedMetricTest.MockContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DDLambdaInstantiationTest {

    private static final String API_GATEWAY_PROXY_TYPE = "com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent";
    private static final String API_GATEWAY_V2_PROXY_TYPE = "com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent";
    private static final String SQS_EVENT_TYPE = "com.amazonaws.services.lambda.runtime.events.SQSEvent";
    private static final String KINESIS_EVENT_TYPE = "com.amazonaws.services.lambda.runtime.events.KinesisEvent";
    private static final String NO_EVENT = "null";
    private final Object event;
    private String eventType;
    private final Context context;
    private final Boolean tracingEnabled;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public DDLambdaInstantiationTest(Object event, Context mockContext, Boolean tracingEnabled) {
        this.event = event;
        this.context = mockContext;
        this.tracingEnabled = tracingEnabled;
    }

    @Parameters
    public static Collection<Object[]> data() {
        APIGatewayProxyRequestEvent dummyAPIGateway = new APIGatewayProxyRequestEvent();
        dummyAPIGateway.setHeaders(new HashMap<String, String>());
        APIGatewayV2ProxyRequestEvent dummyAPIGatewayV2 = new APIGatewayV2ProxyRequestEvent();
        dummyAPIGatewayV2.setHeaders(new HashMap<String, String>());
        return Arrays.asList(new Object[][]{
                //Event, Context, DD_TRACE_ENABLED value
                { dummyAPIGateway, new MockContext(), true },
                { dummyAPIGateway, new MockContext(), false },
                { dummyAPIGatewayV2, new MockContext(), true },
                { dummyAPIGatewayV2, new MockContext(), false },
                { new SQSEvent(), new MockContext(), true },
                { new SQSEvent(), new MockContext(), false },
                { new KinesisEvent(), new MockContext(), true },
                { new KinesisEvent(), new MockContext(), false },
                { null, new MockContext(), true },
                { null, new MockContext(), false },
                { null, null, true },
                { null, null, false },
        });
    }

    @Test
    public void test() {
        if (this.tracingEnabled) {
            environmentVariables.set("DD_TRACE_ENABLED", "true");
        } else {
            environmentVariables.set("DD_TRACE_ENABLED", "false");
        }

        String eventType;
        if (event != null) {
            eventType = this.event.getClass().getTypeName();
        } else {
            eventType = NO_EVENT;
        }

        //Basically, none of these should throw exceptions
        switch (eventType) {
            case API_GATEWAY_PROXY_TYPE:
                APIGatewayProxyRequestEvent proxy = (APIGatewayProxyRequestEvent) this.event;
                DDLambda ddLambdaApiG = new DDLambda(proxy, this.context);
                ddLambdaApiG.finish();
                break;
            case API_GATEWAY_V2_PROXY_TYPE:
                APIGatewayV2ProxyRequestEvent proxy2 = (APIGatewayV2ProxyRequestEvent) this.event;
                DDLambda ddLambdaApiGV2 = new DDLambda(proxy2, this.context);
                ddLambdaApiGV2.finish();
                break;
            case SQS_EVENT_TYPE:
                SQSEvent sqs = (SQSEvent) event;
                DDLambda ddLambdaSQS = new DDLambda(sqs, this.context);
                ddLambdaSQS.finish();
                break;
            case KINESIS_EVENT_TYPE:
                KinesisEvent ke = (KinesisEvent) event;
                DDLambda ddLambdaKinesis = new DDLambda(ke, this.context);
                ddLambdaKinesis.finish();
                break;
            case NO_EVENT:
                DDLambda ddLambdaNone = new DDLambda(this.context);
                ddLambdaNone.finish();
                break;
            default:
                Assert.fail("Unexpected test parameters encountered. " + eventType);
        }

    }
}

