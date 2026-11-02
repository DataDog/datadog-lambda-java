#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2024 Datadog, Inc.

set -e

rm -f dd-java-agent.jar
if [ -n "${TRACER_VERSION:-}" ]; then
  TRACER_URL="https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/${TRACER_VERSION}/dd-java-agent-${TRACER_VERSION}.jar"
  echo "Downloading dd-java-agent ${TRACER_VERSION} from Maven Central (${TRACER_URL})"
else
  TRACER_URL="https://dtdg.co/latest-java-tracer"
  echo "Downloading latest dd-java-agent (${TRACER_URL})"
fi
wget -O dd-java-agent.jar "${TRACER_URL}"

# Extract and clean up the jar
rm -rf jar-temp
mkdir jar-temp
cd jar-temp
unzip -q ../dd-java-agent.jar
rm -f ../dd-java-agent.jar
# Remove unwanted folders
rm -rf profiling && rm -rf iast && rm -rf cws-tls && rm -rf ci-visibility && rm -rf logs-intake

# Remove Windows-specific folders
rm -rf shared/jnr/posix/windows
rm -rf shared/jnr/constants/platform/windows
rm -rf shared/jnr/ffi/provider/jffi/platform/i386/windows
rm -rf shared/jnr/ffi/provider/jffi/platform/aarch64/windows
rm -rf shared/jnr/ffi/provider/jffi/platform/x86_64/windows

# remove unneeded appsec binaries
rm -rf appsec/native_libs/windows
rm -rf appsec/native_libs/macos
rm -rf appsec/native_libs/linux/x86_64/musl
rm -rf appsec/native_libs/linux/aarch64/musl

# Remove Windows-specific files
rm -f shared/jni/i386-Windows/jffi-1.2.dll
rm -f shared/jni/x86_64-Windows/jffi-1.2.dll
rm -f shared/jni/aarch64-Windows/jffi-1.2.dll

# Repackage the cleaned jar (zip with manifest first for proper jar structure)
zip ../dd-java-agent.jar META-INF/MANIFEST.MF
zip -r ../dd-java-agent.jar * -x META-INF/MANIFEST.MF

cd ..
rm -rf jar-temp

rm -rf .layers
mkdir .layers

rm -rf layer-temp
mkdir -p layer-temp/java/lib

cp dd-java-agent.jar layer-temp/java/lib/dd-java-agent.jar
cd layer-temp
zip -r ../.layers/${LAYER_FILE} java

cd ..
rm -rf layer-temp
