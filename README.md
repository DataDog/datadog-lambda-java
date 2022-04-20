# datadog-lambda-java

[![Slack](https://chat.datadoghq.com/badge.svg?bg=632CA6)](https://chat.datadoghq.com/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-java/blob/main/LICENSE)
![](https://github.com/DataDog/datadog-lambda-java/workflows/Test%20on%20Master%20branch/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/com.datadoghq/datadog-lambda-java)

:warning:THIS LIBRARY IS NO LONGER REQUIRED WHEN USING THE LATEST VERSION OF THE DATADOG LAMBDA EXTENSION. IT'S ONLY NEEDED IF YOU WANT TO KEEP INSTRUMENTING USING THE DATADOG FORWARDER.:warning:

:warning: Older versions of `datadog-lambda-java` include `log4j <= 2.14.0` as a transitive dependency. 
We recommend you upgrade to `datadog-lambda-java 0.3.4` or greater, or `1.4.1` or greater.
[Additional upgrade instructions](https://docs.datadoghq.com/serverless/installation/java/?tab=maven#upgrading) are available on our docs site.:warning:

Datadog Lambda Library for Java (8 and 11) enables [enhanced Lambda metrics](https://docs.datadoghq.com/serverless/enhanced_lambda_metrics), [distributed tracing](https://docs.datadoghq.com/serverless/distributed_tracing), and [custom metric submission](https://docs.datadoghq.com/serverless/custom_metrics) from AWS Lambda functions.

## Installation

This library is distributed through [Maven Central](https://search.maven.org/artifact/com.datadoghq/datadog-lambda-java). It is only required when instrumenting using the Datadog Forwarder, see the [installation instructions](https://docs.datadoghq.com/serverless/guide/datadog_forwarder_java/). Also see the latest [installation instructions](https://docs.datadoghq.com/serverless/installation/java/) using the Datadog Lambda extension. 

## Distributed Tracing

### Upstream Requests

You may want to include this Lambda invocation as a child span of some larger trace.
If so, you should anticipate that the event triggering the Lambda will have some trace context attached to it.
If this is the case, then you MUST instantiate `DDLambda` with both the request and the lambda context in order for it to extract the trace context.
E.g. `DDLambda ddl = new DDLambda(request, context);`.
Currently supported events are:

- `APIGatewayProxyRequestEvent`
- `APIGatewayV2ProxyRequestEvent`

If you are using a different event with trace context, you may choose to create a class that implements `Headerable` and supply that as the event instead.

### Downstream Requests

The dd-trace-java tracer will automatically add trace context to outgoing requests for a number of popular services. 
The list of instrumented services can be found here: https://docs.datadoghq.com/tracing/setup_overview/compatibility_requirements/java/ .
If you wish to enable a beta integration, please note that you must do so using an environment variable.

### Trace/Log Correlation

Please see [Connecting Java Logs and Traces](https://docs.datadoghq.com/tracing/connect_logs_and_traces/java/?tab=log4j2)

In brief, if you set the environment variable `DD_LOGS_INJECTION=true`, your trace ID and span ID are automatically injected into the MDC.
If you are using JSON-formatted logs and logging using Logback, there is nothing left to do.

For raw formatted logs, you must update your log format and your log parser. Instructions for both are below.

1. Update Log Format

    If you are using raw formatted logs, update your formatter to include `dd.trace_context`. E.g.

    ```xml
    <Pattern>"%d{yyyy-MM-dd HH:mm:ss} <%X{AWSRequestId}> %-5p %c:%L %X{dd.trace_context} %m%n"</Pattern>
    <!--Please note:                      Request ID                      Trace Context  -->
    ```

    Please note that `RequestId` has also been added. 
    RequestId is not strictly necessary for Trace/Log correlation, but it is useful for correlating logs and invocations.

2. Update Grok Parser

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
