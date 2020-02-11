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