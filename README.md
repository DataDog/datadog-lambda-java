# datadog-lambda-java-library

![CircleCI](https://img.shields.io/circleci/build/github/DataDog/datadog-lambda-layer-js)
[![Code Coverage](https://img.shields.io/codecov/c/github/DataDog/datadog-lambda-layer-js)](https://codecov.io/gh/DataDog/datadog-lambda-layer-js)
[![NPM](https://img.shields.io/npm/v/datadog-lambda-js)](https://www.npmjs.com/package/datadog-lambda-js)
[![Slack](https://img.shields.io/badge/slack-%23serverless-blueviolet?logo=slack)](https://datadoghq.slack.com/channels/serverless/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](https://github.com/DataDog/datadog-lambda-layer-js/blob/master/LICENSE)

The Datadog Lambda Java Client Library enables distributed tracing between serverful and serverless environments, as well as letting you send custom metrics to the Datadog API.

## Installation

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


## Environment Variables

You can set the following environment variables via the AWS CLI or Serverless Framework

### DD_LOG_LEVEL

How much logging datadog-lambda-layer-js should do. Set this to "debug" for extensive logs.

### DD_ENHANCED_METRICS

If you set the value of this variable to "true" then the Lambda layer will increment a Lambda integration metric called `aws.lambda.enhanced.invocations` with each invocation and `aws.lambda.enhanced.errors` if the invocation results in an error. These metrics are tagged with the function name, region, account, runtime, memorysize, and `cold_start:true|false`.

### DD_LOGS_INJECTION

*Always assume TRUE: Confirm with Tian*

To connect logs and traces, set the environment variable `DD_LOGS_INJECTION` to `true` if `console` is used for logging. For other logging libraries, see instructions for [automatic](https://docs.datadoghq.com/tracing/advanced/connect_logs_and_traces/?tab=nodejs#automatic-trace-id-injection) and [manual](https://docs.datadoghq.com/tracing/advanced/connect_logs_and_traces/?tab=nodejs#manual-trace-id-injection) trace id injection.

## Usage

Datadog needs to be able to read headers from the incoming Lambda event.

```typescript
//////////////////////////////////////////////
//////////////////////////////////////////////
//Update to Java Code! Have Chris take a look.
//////////////////////////////////////////////
//////////////////////////////////////////////

const { datadog } = require("datadog-lambda-js");

async function myHandler(event, context) {
  return {
    statusCode: 200,
    body: "hello, dog!",
  };
}
// Wrap your handler function like this
module.exports.myHandler = datadog(myHandler);
/* OR with manual configuration options
module.exports.myHandler = datadog(myHandler, {
    apiKey: "my-api-key"
});
*/
```

## Custom Metrics

Custom metrics can be submitted using the `sendDistributionMetric` function. The metrics are submitted as [distribution metrics](https://docs.datadoghq.com/graphing/metrics/distributions/).

**IMPORTANT NOTE:** If you have already been submitting the same custom metric as non-distribution metric (e.g., gauge, count, or histogram) without using the Datadog Lambda Layer, you MUST pick a new metric name to use for `sendDistributionMetric`. Otherwise that existing metric will be converted to a distribution metric and the historical data prior to the conversion will be no longer queryable.

```typescript
//////////////////////////////////////////////
//////////////////////////////////////////////
//Update to JAVA Code! Have Chris take a look.
//////////////////////////////////////////////
//////////////////////////////////////////////

const { sendDistributionMetric } = require("datadog-lambda-js");

sendDistributionMetric(
  "coffee_house.order_value", // Metric name
  12.45, // The Value
  "product:latte",
  "order:online", // Associated tags
);
```

## Distributed Tracing

*This might not be complete, clarrify with Chris. [ON HOLD TILL CHRIS TESTS]*

[Distributed tracing](https://docs.datadoghq.com/tracing/guide/distributed_tracing/?tab=nodejs) allows you to propagate a trace context from a service running on a host to a service running on AWS Lambda, and vice versa, so you can see performance end-to-end. Linking is implemented by injecting Datadog trace context into the HTTP request headers.

Distributed tracing headers are language agnostic, e.g., a trace can be propagated between a Java service running on a host to a Lambda function written in Node.

Because the trace context is propagated through HTTP request headers, the Lambda function needs to be triggered by AWS API Gateway or AWS Application Load Balancer.

*This might not be accurate, confirm with Chris.*

To enable this feature wrap your handler functions using the `datadog` function.

### Sampling

The traces for your Lambda function are converted by Datadog from AWS X-Ray traces. X-Ray needs to sample the traces that the Datadog tracing agent decides to sample, in order to collect as many complete traces as possible. You can create X-Ray sampling rules to ensure requests with header `x-datadog-sampling-priority:1` or `x-datadog-sampling-priority:2` via API Gateway always get sampled by X-Ray.

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

### Non-proxy integration

*Chris will clarrify*

If your Lambda function is triggered by API Gateway via the non-proxy integration, then you have to set up a mapping template, which passes the Datadog trace context from the incoming HTTP request headers to the Lambda function via the event object.

If your Lambda function is deployed by the Serverless Framework, such a mapping template gets created by default.

## Opening Issues

If you encounter a bug with this package, we want to hear about it. Before opening a new issue, search the existing issues to avoid duplicates.

When opening an issue, include the Datadog Lambda Layer version, Java version, and stack trace if available. In addition, include the steps to reproduce when appropriate.

You can also open an issue for a feature request.

## Contributing

If you find an issue with this package and have a fix, please feel free to open a pull request following the [procedures](https://github.com/DataDog/dd-lambda-layer-js/blob/master/CONTRIBUTING.md).

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.

This product includes software developed at Datadog (https://www.datadoghq.com/). Copyright 2019 Datadog, Inc.
