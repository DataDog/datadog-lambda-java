Datadog Lambda Java Client Library 
============================================

The Datadog Lambda Java Client Library enables distributed tracing between serverful
and serverless environments, as well as letting you send custom metrics to the
Datadog API.

Features
--------

- [x] Custom Metrics
- [x] Enhanced Metrics
- [ ] Distributed Tracing

TODO
----

- [x] Implement custom metrics
- [x] Implement enhanced metrics 
- [x] Implement internal log levels/output
- [ ] Implement tracing
  - [x] Get DD Trace Context from API Gateway Request, if possible, and add a dummy
  segment to the X-Ray trace.
  - [ ] Wrapper for adding trace context into HTTP requests (break them off into their 
  own spans?)
  - [ ] Write traces to logs on logger flush
- [ ] Build and Deploy
  - [x] Test & build jar with Gradle
  - [ ] Scripts to automate testing and jar building (Dockerize)
  - [ ] CI checks
  - [ ] (Script to?) push new version to Maven Central
  - [ ] Document dev/build/release/deploy steps in SLS-team wiki

Installation
------------

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

Environment Variables
---------------------

- `DD_LOG_LEVEL`: The level at which the Datadog library will emit its own log messages.
Possible values are `debug`, `info`, `warn` or `error`.

Distributed Tracing
-------------------

Wrap your outbound HTTP requests with trace headers to see your lambda in context in APM.
The Lambda Java Client Library supports adding headers to the following HTTP Clients:

- java.net.HttpUrlConnection
- Apache HTTP Client
- OKHttp3

Don't see your favorite client? Open an issue and request it. Datadog is adding to 
this library all the time.

### HttpUrlConnection example

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        LambdaInstrumenter li = new LambdaInstrumenter(request, lambda);
 
        URL url = new URL("https://example.com");
        HttpURLConnection hc = (HttpURLConnection)url.openConnection();

        //Add the distributed tracing headers
        hc = (HttpURLConnection) li.addTraceHeaders(hc);

        hc.connect();
    
        return 7;
    }
}
```

### Apache HTTP Client example

```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        LambdaInstrumenter li = new LambdaInstrumenter(request, lambda);
    
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet hg = new HttpGet("https://example.com");
    
        //Add the distributed tracing headers
        hg = (HttpGet) li.addTraceHeaders(hg);

        HttpResponse hr = client.execute(hg);
        return 7;
    }
}
```


### OKHttp3 Client example


```java
public class Handler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayV2ProxyResponseEvent> {
    public Integer handleRequest(APIGatewayV2ProxyRequestEvent request, Context context){
        LambdaInstrumenter li = new LambdaInstrumenter(request, lambda);
    
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
