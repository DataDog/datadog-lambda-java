Datadog Lambda Java Client Library 
============================================

[![Slack](https://img.shields.io/badge/slack-%23serverless-blueviolet?logo=slack)](https://datadoghq.slack.com/channels/serverless/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-java/blob/master/LICENSE)

The Datadog Lambda Java Client Library enables distributed tracing between serverful
and serverless environments, as well as letting you send custom metrics to the
Datadog API.


Installation
------------

This library is provided as a Java Library via Maven Central.

### Maven

Something something `pom.xml`.

```$xml
<blah>
```

### Gradle

Include the following dependency in your `build.gradle`.

```groovy
dependencies {
        implementation 'com.datadoghq.datadog-lambda-layer-java:0.1.0'
}
```

Usage
-----

Create a new `DDLambda` with your request (optional, but recommended) and Lambda context. 

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda); //Records your lambda invocation, 
   
        int work = DoWork();
        dd.metric("work.done", work);
        
        return work;
    }
}
```


## Environment Variables

You can set the following environment variables via the AWS CLI or Serverless Framework

### DD_LOG_LEVEL

How much logging datadog-lambda-layer-js should do. Set this to "debug" for extensive logs.

### DD_ENHANCED_METRICS

If you set the value of this variable to "true" then the Lambda layer will increment a Lambda integration metric called `aws.lambda.enhanced.invocations` with each invocation and `aws.lambda.enhanced.errors` if the invocation results in an error. These metrics are tagged with the function name, region, account, runtime, memorysize, and `cold_start:true|false`.

### DD_LOGS_INJECTION

*Always assume TRUE: Confirm with Tian*

To connect logs and traces, set the environment variable `DD_LOGS_INJECTION` to `true` if `console` is used for logging. For other logging libraries, see instructions for [automatic](https://docs.datadoghq.com/tracing/advanced/connect_logs_and_traces/?tab=nodejs#automatic-trace-id-injection) and [manual](https://docs.datadoghq.com/tracing/advanced/connect_logs_and_traces/?tab=nodejs#manual-trace-id-injection) trace id injection.

## Custom Metrics

Custom metrics can be submitted using the `metric` function. The metrics are submitted as 
[distribution metrics](https://docs.datadoghq.com/graphing/metrics/distributions/).

**IMPORTANT NOTE:** If you have already been submitting the same custom metric as non-distribution metric
 (e.g., gauge, count, or histogram) without using the Datadog Lambda Layer, you MUST pick a new metric
  name to use for `metric`. Otherwise that existing metric will be converted to a distribution metric 
  and the historical data prior to the conversion will be no longer queryable.

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        DDLambda dd = new DDLambda(request, lambda); //Records your lambda invocation, 
   
        int work = DoWork();
        dd.metric("work.done", work); // Submit a custom metric about this work you've done.
        
        return work;
    }
}
```

Distributed Tracing
-------------------

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
        HttpURLConnection instrumentedUrlConnection = li.makeUrlConnection(url); //Trace headers included

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
        hc = (HttpURLConnection) li.addTraceHeaders(hc);

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
    
        HttpGet hg = li.makeHttpGet("https://example.com"); //Trace headers included

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
        hg = (HttpGet) li.addTraceHeaders(hg);

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
        Request okHttpRequest = li.makeRequestBuilder() // Trace headers included
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
        okHttpRequest = li.addTraceHeaders(okHttpRequest);

        Response resp = okHttpClient.newCall(okHttpRequest).execute();

        return 7;
    }
}
```


Custom Non-Proxy API Gateway Event support
------------------------------------------

AWS API Gateway allows you to map information about your API Gateway request onto a 
[custom POJO](https://docs.aws.amazon.com/lambda/latest/dg/java-handler-io-type-pojo.html).
If you choose to do this, and if you want to support Datadog distributed tracing, your 
custom POJO must implement `com.datadoghq.datadog_lambda_java.Headerable`. Additionally,
API Gateway must map the request headers to the headers field of your custom POJO.

Example Custom POJO

```java
package com.example.my_sample_lambda;

import com.datadoghq.datadog_lambda_java.Headerable;
import java.util.Map;

public class CustomRequest implements Headerable {
    Map<String,String> headers;

    public CustomRequest(){}

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

}

public class Handler implements RequestHandler<CustomRequest, APIGatewayV2ProxyResponseEvent> {
	@Override
	public String handleRequest(CustomRequest request, Context context) {
        String xForwardedFor = request.getHeaders().get("X-Forwarded-For");
        return "{\"result\":\"" + xForwardedFor + "\"}";
    }
}
```

Your API Gateway mapping template must write the request headers into the `headers` 
field of your POJO. The [Serverless framework](https://serverless.com/) comes with default mapping templates 
for `application/JSON` and `application/x-www-form-urlencoded` that conform with Headerable.

A minimum viable mapping template would look something like this:

```vtl
  #define( $loop )
    {
    #foreach($key in $map.keySet())
        #set( $k = $util.escapeJavaScript($key) )
        #set( $v = $util.escapeJavaScript($map.get($key)).replaceAll("\\'", "'") )
        "$k":
          "$v"
          #if( $foreach.hasNext ) , #end
    #end
    }
  #end

{
    #set( $map = $input.params().header )
    "headers": $loop,
}
```

Sampling
--------

The traces for your Lambda function are converted by Datadog from AWS X-Ray traces. X-Ray needs to 
sample the traces that the Datadog tracing agent decides to sample, in order to collect as many 
complete traces as possible. You can create X-Ray sampling rules to ensure requests with header 
`x-datadog-sampling-priority:1` or `x-datadog-sampling-priority:2` via API Gateway always get sampled 
by X-Ray.

These rules can be created using the following AWS CLI command.

```bash
aws xray create-sampling-rule --cli-input-json file://datadog-sampling-priority-1.json
aws xray create-sampling-rule --cli-input-json file://datadog-sampling-priority-2.json
```

The file content for `datadog-sampling-priority-1.json`:

```json
{
  "SamplingRule": {
    "RuleName": "Datadog-Sampling-Priority-1",
    "ResourceARN": "*",
    "Priority": 9998,
    "FixedRate": 1,
    "ReservoirSize": 100,
    "ServiceName": "*",
    "ServiceType": "AWS::APIGateway::Stage",
    "Host": "*",
    "HTTPMethod": "*",
    "URLPath": "*",
    "Version": 1,
    "Attributes": {
      "x-datadog-sampling-priority": "1"
    }
  }
}
```

The file content for `datadog-sampling-priority-2.json`:

```json
{
  "SamplingRule": {
    "RuleName": "Datadog-Sampling-Priority-2",
    "ResourceARN": "*",
    "Priority": 9999,
    "FixedRate": 1,
    "ReservoirSize": 100,
    "ServiceName": "*",
    "ServiceType": "AWS::APIGateway::Stage",
    "Host": "*",
    "HTTPMethod": "*",
    "URLPath": "*",
    "Version": 1,
    "Attributes": {
      "x-datadog-sampling-priority": "2"
    }
  }
}
```


Opening Issues
--------------

If you encounter a bug with this package, we want to hear about it. Before opening a new issue, 
search the existing issues to avoid duplicates.

When opening an issue, include the Datadog Lambda Layer version, Java version, and stack trace if 
available. In addition, include the steps to reproduce when appropriate.

You can also open an issue for a feature request.

Contributing
------------

If you find an issue with this package and have a fix, please feel free to open a pull request 
following the [procedures](https://github.com/DataDog/dd-lambda-layer-js/blob/master/CONTRIBUTING.md).

License
-------

Unless explicitly stated otherwise all files in this repository are licensed under the Apache 
License Version 2.0.

This product includes software developed at Datadog (https://www.datadoghq.com/). Copyright 2019 
Datadog, Inc.