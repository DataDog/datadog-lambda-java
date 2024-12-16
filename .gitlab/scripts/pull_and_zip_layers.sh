#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2024 Datadog, Inc.

set -e

rm -f dd-java-agent.jar
wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer

rm -rf .layers
mkdir .layers

rm -rf layer-temp
mkdir -p layer-temp/java/lib

cp dd-java-agent.jar layer-temp/java/lib/dd-java-agent.jar
cd layer-temp
zip -r ../.layers/tracer.zip java
cd ..
rm -rf layer-temp
