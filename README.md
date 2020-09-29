# datadog-lambda-java

[![Slack](https://img.shields.io/badge/slack-%23serverless-blueviolet?logo=slack)](https://datadoghq.slack.com/channels/serverless/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-java/blob/master/LICENSE)
![](https://github.com/DataDog/datadog-lambda-java/workflows/Test%20on%20Master%20branch/badge.svg)
![Bintray](https://img.shields.io/bintray/v/datadog/datadog-maven/datadog-lambda-java)

The Datadog Lambda Java Client Library for Java (8 and 11) enables [enhanced lambda metrics](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#real-time-enhanced-lambda-metrics) 
and [distributed tracing](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#tracing-with-datadog-apm) 
between serverful and serverless environments, as well as letting you send 
[custom metrics](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#custom-metrics) 
to the Datadog API.


## Installation

This library will be distributed through JFrog [Bintray](https://bintray.com/beta/#/datadog/datadog-maven/datadog-lambda-java). Follow the [installation instructions](https://docs.datadoghq.com/serverless/installation/java/), and view your function's enhanced metrics, traces and logs in Datadog. 

## Environment Variables

### DD_LOG_LEVEL

Set to `debug` enable debug logs from the Datadog Lambda Library. Defaults to `info`.

### DD_ENHANCED_METRICS

Generate enhanced Datadog Lambda integration metrics, such as, `aws.lambda.enhanced.invocations` and `aws.lambda.enhanced.errors`. Defaults to `true`.

## Enhanced Metrics

Once [installed](#installation), you should be able to view enhanced metrics for your Lambda function in Datadog.

Check out the official documentation on [Datadog Lambda enhanced metrics](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=java#real-time-enhanced-lambda-metrics).

## Custom Metrics

Once [installed](#installation), you should be able to submit custom metrics from your Lambda function.

Check out the instructions for [submitting custom metrics from AWS Lambda functions](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=java#custom-metrics).

## Tracing

Wrap your outbound HTTP requests with trace headers to see your lambda in context in APM.
The Lambda Java Client Library provides instrumented HTTP connection objects as well as helper methods for
instrumenting HTTP connections made with any of the following libraries:

- java.net.HttpUrlConnection
- Apache HTTP Client
- OKHttp3

Don't see your favorite client? Open an issue and request it. Datadog is adding to 
this library all the time.

### HttpUrlConnection examples

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
 
        URL url = new URL("https://example.com");
        HttpURLConnection instrumentedUrlConnection = dd.makeUrlConnection(url); //Trace headers included

        instrumentedUrlConnection.connect();
    
        return 7;
    }
}
```

Alternatively, if you want to do something more complex:

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
 
        URL url = new URL("https://example.com");
        HttpURLConnection hc = (HttpURLConnection)url.openConnection();

        //Add the distributed tracing headers
        hc = (HttpURLConnection) dd.addTraceHeaders(hc);

        hc.connect();
    
        return 7;
    }
}
```

### Apache HTTP Client examples

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
    
        HttpClient client = HttpClientBuilder.create().build();
    
        HttpGet hg = dd.makeHttpGet("https://example.com"); //Trace headers included

        HttpResponse hr = client.execute(hg);
        return 7;
    }
}
```

Alternatively, if you want to do something more complex:

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
    
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet hg = new HttpGet("https://example.com");
    
        //Add the distributed tracing headers
        hg = (HttpGet) dd.addTraceHeaders(hg);

        HttpResponse hr = client.execute(hg);
        return 7;
    }
}
```


### OKHttp3 Client examples

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
    
        HttpClient client = HttpClientBuilder.create().build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request okHttpRequest = dd.makeRequestBuilder() // Trace headers included
            .url("https://example.com")
            .build(); 

        Response resp = okHttpClient.newCall(okHttpRequest).execute();

        return 7;
    }
}
```

Alternatively:

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda);
    
        HttpClient client = HttpClientBuilder.create().build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request okHttpRequest = new Request.Builder()
            .url("https://example.com")
            .build();

        //Add the distributed tracing headers
        okHttpRequest = dd.addTraceHeaders(okHttpRequest);

        Response resp = okHttpClient.newCall(okHttpRequest).execute();

        return 7;
    }
}
```

## Opening Issues

If you encounter a bug with this package, we want to hear about it. Before opening a new issue, 
search the existing issues to avoid duplicates.

When opening an issue, include the Datadog Lambda Layer version, Java version, and stack trace if 
available. In addition, include the steps to reproduce when appropriate.

You can also open an issue for a feature request.

## Contributing

If you find an issue with this package and have a fix, please feel free to open a pull request 
following the [procedures](https://github.com/DataDog/dd-lambda-layer-js/blob/master/CONTRIBUTING.md).

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.

This product includes software developed at Datadog (https://www.datadoghq.com/). Copyright 2020 Datadog, Inc.
