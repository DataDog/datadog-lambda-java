#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# The stock gradle:7.1.0-jdk8 docker image has everything we need to build this project.
# No need for a Dockerfile
docker run --rm \
  -v $(pwd):/datadog-lambda-java \
  -w /datadog-lambda-java \
  gradle:7.1.0-jdk8 \
  ./gradlew test
