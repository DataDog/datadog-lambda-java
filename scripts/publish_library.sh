#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

if [[ ${SONATYPE_USERNAME-} == "" ]]
then
  echo "SONATYPE_USERNAME required for publishing"
  exit 1
fi


if [[ ${SONATYPE_PASSWORD-} == "" ]]
then
  echo "SONATYPE_PASSWORD required for publishing"
  exit 1
fi

# The stock gradle:7.1.0-jdk8 docker image has everything we need to build this project.
# No need for a Dockerfile
docker run --rm -v "$(pwd)":/datadog-lambda-java \
  -w /datadog-lambda-java\
  -e SONATYPE_USERNAME \
  -e SONATYPE_PASSWORD \
  gradle:7.1.0-jdk8 \
  ./gradlew clean GenerateBuildConfig build uploadArchives

echo ""
echo "The library has been published to the Staging Repository"
echo "Please visit https://oss.sonatype.org/#stagingRepositories to release"
