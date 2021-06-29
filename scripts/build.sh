#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# The stock gradle:7.1.0-jdk8 docker image has everything we need to build this project.
# No need for a Dockerfile.

# The build target runs tests as well.
docker run --rm \
  -v $(pwd):/datadog-lambda-java \
  -w /datadog-lambda-java \
  gradle:7.1.0-jdk8 \
  ./gradlew clean GenerateBuildConfig build

echo ""
echo "Build succeeded."
echo "Check build/libs/ for the build artifacts"
