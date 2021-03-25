#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2021 Datadog, Inc.

# Publish the dd-trace-java lambda layer across regions, using the AWS CLI
# Usage: VERSION=3 REGIONS=us-east-1 publish_layers.sh
# VERSION is required.
set -e

LAYER_NAME="dd-trace-java"
AVAILABLE_REGIONS=$(aws ec2 describe-regions --output json | jq -r '.[] | .[] | .RegionName')

# Determine the target regions
if [ -z "$REGIONS" ]; then
    echo "Region not specified, running for all available regions."
    REGIONS=$AVAILABLE_REGIONS
else
    echo "Region specified: $REGIONS"
    if [[ ! "$AVAILABLE_REGIONS" == *"$REGIONS"* ]]; then
        echo "Could not find $REGIONS in available regions: $AVAILABLE_REGIONS"
        echo ""
        echo "EXITING SCRIPT."
        exit 1
    fi
fi

# Determine the target layer version
if [ -z "$VERSION" ]; then
    echo "Layer version not specified"
    echo ""
    echo "EXITING SCRIPT."
    exit 1
else
    echo "Layer version specified: $VERSION"
fi

LATEST_JAR_URL=$(curl -i https://repository.sonatype.org/service/local/artifact/maven/redirect\?r\=central-proxy\&g\=com.datadoghq\&a\=dd-java-agent\&v\=LATEST | grep location | cut -d " " -f 2)

LATEST_JAR_FILE=$(echo $LATEST_JAR_URL | rev | cut -d "/" -f 1 | rev)

echo ""
read -p "Ready to publish layer version $VERSION to regions ${REGIONS[*]} (y/n)?" CONT
if [ "$CONT" != "y" ]; then
    echo "Exiting"
    exit 1
fi

wget -O $LATEST_JAR_FILE https://dtdg.co/latest-java-tracer

LAYER_ZIP="tracer.zip"

rm -rf layer
mkdir -p layer/java/lib
cp "$LATEST_JAR_FILE" layer/java/lib/dd-java-agent.jar
cd layer
zip -r ../$LAYER_ZIP java
cd ..
rm -rf layer

# sign the layer zip

echo
echo "Signing layers..."
./scripts/sign_layers.sh prod $LAYER_ZIP

publish_layer() {
    region=$1
    version_nbr=$(aws lambda publish-layer-version --layer-name $LAYER_NAME \
        --description "Datadog Tracer Lambda Layer for Java" \
        --zip-file "fileb://tracer.zip" \
        --region $region | jq -r '.Version')

    permission=$(aws lambda add-layer-version-permission --layer-name $LAYER_NAME \
        --version-number $version_nbr \
        --statement-id "release-$version_nbr" \
        --action lambda:GetLayerVersion --principal "*" \
        --region $region)

    echo $version_nbr
}

for region in $REGIONS
do
    echo "Starting publishing layer for region $region..."
    latest_version=$(aws lambda list-layer-versions --region $region --layer-name $LAYER_NAME --query 'LayerVersions[0].Version || `0`')
    if [ $latest_version -ge $VERSION ]; then
        echo "Layer $LAYER_NAME version $VERSION already exists in region $region, skipping..."
        continue
    elif [ $latest_version -lt $((VERSION-1)) ]; then
        read -p "WARNING: The latest version of layer $LAYER_NAME in region $region is $latest_version, publish all the missing versions including $VERSION or EXIT the script (y/n)?" CONT
        if [ "$CONT" != "y" ]; then
            echo "Exiting"
            exit 1
        fi
    fi
    while [ $latest_version -lt $VERSION ]; do
        latest_version=$(publish_layer $region)
        echo "Published version $latest_version of layer in region $region"

        # This shouldn't happen unless someone manually deleted the latest version, say 28, and
        # then tries to republish 28 again. The published version would actually be 29, because
        # Lambda layers are immutable and AWS will skip deleted version and use the next number. 
        if [ $latest_version -gt $VERSION ]; then
            echo "ERROR: Published version $latest_version is greater than the desired version $VERSION!"
            echo "Exiting"
            exit 1
        fi
    done
done

echo "Done !"
