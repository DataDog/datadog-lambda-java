#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2021 Datadog, Inc.

# Publish the datadog python lambda layer across regions, using the AWS CLI
# Usage: publish_layer.sh [region] [layer]
# Specifying the region and layer arg will publish the specified layer to the specified region
set -e

# Makes sure any subprocesses will be terminated with this process
trap "pkill -P $$; exit 1;" INT

LATEST_JAR_URL=$(curl -i https://repository.sonatype.org/service/local/artifact/maven/redirect\?r\=central-proxy\&g\=com.datadoghq\&a\=dd-java-agent\&v\=LATEST | grep location | cut -d " " -f 2)

LATEST_JAR_FILE=$(echo $LATEST_JAR_URL | rev | cut -d "/" -f 1 | rev)

echo ""
echo "About to publish a new Lambda layer containing $LATEST_JAR_FILE"
read -r -p "Continue? [y/N] " response
if ! [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]
then
   echo "OK, exiting!"
   exit 0
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


AVAILABLE_REGIONS=$(aws ec2 describe-regions --output json | jq -r '.[] | .[] | .RegionName')


# Check region arg
if [ -z "$1" ]; then
    echo "Region parameter not specified, running for all available regions."
    REGIONS=$AVAILABLE_REGIONS
else
    echo "Region parameter specified: $1"
    if [[ ! "$AVAILABLE_REGIONS" == *"$1"* ]]; then
        echo "Could not find $1 in available regions: $AVAILABLE_REGIONS"
        echo ""
        echo "EXITING SCRIPT."
        exit 1
    fi
    REGIONS=($1)
fi

echo "Starting publishing layers for regions: $REGIONS"


publish_layer() {
    region=$1
    layer_name="dd-trace-java"
    aws_version_key=$3
    layer_path=$4
    version_nbr=$(aws lambda publish-layer-version --layer-name $layer_name \
        --description "Datadog Tracer Lambda Layer for Java" \
        --zip-file "fileb://tracer.zip" \
        --region $region \
	--output json \
                        | jq -r '.Version')

    aws lambda add-layer-version-permission --layer-name $layer_name \
        --version-number $version_nbr \
        --statement-id "release-$version_nbr" \
        --action lambda:GetLayerVersion --principal "*" \
        --region $region\
    	--output json

    echo "Published layer for region $region, layer_name $layer_name, layer_version $version_nbr"
}

for region in $REGIONS
do
    echo "Starting publishing layer for region $region..."

    publish_layer $region $layer_name $aws_version_key
done


echo "Done !"
