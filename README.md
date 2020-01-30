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
- [ ] Implement internal log levels/output
- [ ] Implement tracing
  - [ ] Start span on Lambda invocation
  - [ ] Get DD Trace Context from API Gateway Request, if possible
  - [ ] Wrapper for adding trace context into HTTP requests (break them off into their 
  own spans?)
  - [ ] Include sampling info
  - [ ] Write traces to logs on logger flush

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