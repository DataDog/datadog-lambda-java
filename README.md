# datadog-lambda-java

[![Slack](https://chat.datadoghq.com/badge.svg?bg=632CA6)](https://chat.datadoghq.com/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-java/blob/main/LICENSE)
![](https://github.com/DataDog/datadog-lambda-java/workflows/Test%20on%20Master%20branch/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/com.datadoghq/datadog-lambda-java)

The Datadog Lambda Java Client Library for Java (8 and 11) enables [enhanced lambda metrics](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#real-time-enhanced-lambda-metrics) 
and [distributed tracing](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#tracing-with-datadog-apm) 
between serverful and serverless environments, as well as letting you send 
[custom metrics](https://docs.datadoghq.com/integrations/amazon_lambda/?tab=awsconsole#custom-metrics) 
to the Datadog API.

:warning: Older versions of `datadog-lambda-java` include `log4j <= 2.14.0` as a transitive dependency. 
We recommend you upgrade to `datadog-lambda-java 0.3.4` or greater, or `1.4.1` or greater.
[Additional upgrade instructions](https://docs.datadoghq.com/serverless/installation/java/?tab=maven#upgrading) are available on our docs site.

## Installation

This library will be distributed through [Maven Central](https://search.maven.org/artifact/com.datadoghq/datadog-lambda-java). 
Follow the [installation instructions](https://docs.datadoghq.com/serverless/installation/java/), and view your function's enhanced metrics, traces and logs in Datadog. 

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

## Installing the Java Tracer

The [Java Tracer](https://docs.datadoghq.com/tracing/setup_overview/setup/java/?tab=containers)
is an optional component that allows you to trace the execution of your Java Lambda function. 
The traces will be viewable from your Serverless function details page within Datadog.

Briefly, in order to use the Java tracer, the following prerquisites must be met (detailed below):
1. The `datadog-lambda-java` library must be included in your project, following these  [installation instructions](https://docs.datadoghq.com/serverless/installation/java/)
1. You must use a compatible Java runtime
1. You must attach the Java Tracer lambda layer
1. Several environment variables must be set
1. Your handler must instantiate a `new DDLambda` in order to start traces, and call `DDLambda#finish` in order to end traces.
 

### Cold start considerations

The Java Tracer adds a nontrivial cold start penalty. 
Expect roughly 6 seconds per cold start if your Lambda function is configured with 3008MB of memory.
Lambda runtime CPU scales with the amount of memory allocated, so allocating more memory may  help alleviate cold start issues.
Also consider using provisioned concurrency to keep your lambda function warm.

### Compatible Java runtimes

- java8.al2 (aka Java 8 (Corretto))
- java11 (aka Java 11 (Corretto))

If your lambda function is using Java 8, please change it to Java 8 Corretto.
It's called java8.al2 if you're editing serverless.yaml.

### Required Lambda Layer containing the Java Tracer

```
arn:aws:lambda:[REGION]:464622532012:layer:dd-trace-java:2
```

The lambda layer version (in this case, `2`) will always correspond with the minor version of the `datadog-lambda-java` library.

### Required environment variables for the Java Tracer

```bash
JAVA_TOOL_OPTIONS: "-javaagent:\"/opt/java/lib/dd-java-agent.jar\""
DD_LOGS_INJECTION: "true"
DD_JMXFETCH_ENABLED: "false"
DD_TRACE_ENABLED: "true"
```

### Required code modification for the Java Tracer

In order to use the Java Tracer, you must instantiate a new `DDLambda` at the beginning of your Lambda function and call `DDLambda#finish()` at the end of it.

```java
public class Handler{

  public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent input, Context context){
    DDLambda ddl = new DDLambda(input, context); // required to set various tags inside the tracer

    ddl.metric("foo.bar", 42, null);
    do_some_stuff();
    make_some_http_requests();

    ddl.finish(); // Required to complete the trace
    return new ApiGatewayResponse();
  }
}
```

`DDLambda ddl = new DDLambda(input, context);` starts a new trace (if the Java Tracer agent is attached to the JRE)
and sets tags based on the Lambda context. If there is a trace context attached to the request, that will be used
to set the trace ID and the parent of the span.

`ddl.finish();` finishes the active span and closes the active trace scope. 
The tracer will flush the trace to Cloudwatch logs once this is called.

# Distributed Tracing

## Upstream Requests

You may want to include this Lambda invocation as a child span of some larger trace.
If so, you should anticipate that the event triggering the Lambda will have some trace context attached to it.
If this is the case, then you MUST instantiate `DDLambda` with both the request and the lambda context in order for it to extract the trace context.
E.g. `DDLambda ddl = new DDLambda(request, context);`.
Currently supported events are:

- `APIGatewayProxyRequestEvent`
- `APIGatewayV2ProxyRequestEvent`

If you are using a different event with trace context, you may choose to create a class that implements `Headerable` and supply that as the event instead.

## Downstream Requests

The dd-trace-java tracer will automatically add trace context to outgoing requests for a number of popular services. 
The list of instrumented services can be found here: https://docs.datadoghq.com/tracing/setup_overview/compatibility_requirements/java/ .
If you wish to enable a beta integration, please note that you must do so using an environment variable.

# Trace/Log Correlation

Please see [Connecting Java Logs and Traces](https://docs.datadoghq.com/tracing/connect_logs_and_traces/java/?tab=log4j2)

In brief, if you set the environment variable `DD_LOGS_INJECTION=true`, your trace ID and span ID are automatically injected into the MDC.
If you are using JSON-formatted logs and logging using Logback, there is nothing left to do.

## Raw-formatted logs (Log4J, etc.)

For raw formatted logs, you must update your log format and your log parser. Instructions for both are below.

### Log Format

If you are using raw formatted logs, update your formatter to include `dd.trace_context`. E.g.

```xml
<Pattern>"%d{yyyy-MM-dd HH:mm:ss} <%X{AWSRequestId}> %-5p %c:%L %X{dd.trace_context} %m%n"</Pattern>
<!--Please note:                      Request ID                      Trace Context  -->
```

Please note that `RequestId` has also been added. 
RequestId is not strictly necessary for Trace/Log correlation, but it is useful for correlating logs and invocations.


### Grok Parser

The following grok parser parses Java logs formatted using the pattern in the previous section.

```
java_tracer %{date("yyyy-MM-dd HH:mm:ss"):timestamp}\s\<%{uuid:lambda.request_id}\>\s%{word:level}\s+%{data:call_site}%{_trace_context}%{data:message}
```

## Opening Issues

If you encounter a bug with this package, we want to hear about it. Before opening a new issue, 
search the existing issues to avoid duplicates.

When opening an issue, include the Datadog Lambda Layer version, Java version, and stack trace if 
available. In addition, include the steps to reproduce when appropriate.

You can also open an issue for a feature request.

## Contributing

If you find an issue with this package and have a fix, please feel free to open a pull request 
following the [procedures](https://github.com/DataDog/datadog-lambda-java/blob/main/CONTRIBUTING.md).

## Community

For product feedback and questions, join the `#serverless` channel in the [Datadog community on Slack](https://chat.datadoghq.com/).

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.

This product includes software developed at Datadog (https://www.datadoghq.com/). Copyright 2020 Datadog, Inc.
