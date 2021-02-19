# datadog-lambda-java

[![Slack](https://chat.datadoghq.com/badge.svg?bg=632CA6)](https://chat.datadoghq.com/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-java/blob/main/LICENSE)
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
The list of instrument services can be found here: https://docs.datadoghq.com/tracing/setup_overview/compatibility_requirements/java/ .
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
