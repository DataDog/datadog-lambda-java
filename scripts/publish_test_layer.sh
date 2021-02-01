#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2021 Datadog, Inc.

# Publish the datadog python lambda layer across regions, using the AWS CLI
# Usage: publish_test_layer.sh path/to/layer.jar [region]
# Specifying the region and layer arg will publish the specified layer to the specified region
set -e

# Makes sure any subprocesses will be terminated with this process
trap "pkill -P $$; exit 1;" INT

if [ -z "$1" ]
then
  echo "Usage: publish_test_layer path/to/layerFile.jar [region]"
  exit 0
fi


LAYER_FILE=$1
REGION=$2
LAYER_ZIP="tracer.zip"

if [ -z "$REGION" ]
then
  REGION="sa-east-1"
fi

echo ""
echo "About to publish a TEST Lambda layer containing $LAYER_FILE to $REGION"
read -r -p "Continue? [y/N] " response
if ! [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]
then
   echo "OK, exiting!"
   exit 0
fi

rm -rf layer
mkdir -p layer/java/lib
cp "$LAYER_FILE" layer/java/lib/dd-java-agent.jar
cd layer
zip -r ../$LAYER_ZIP java
cd ..
rm -rf layer

echo
echo "Signing layer..."
./scripts/sign_layers.sh sandbox $LAYER_ZIP

AVAILABLE_REGIONS=$(aws ec2 describe-regions --output json | jq -r '.[] | .[] | .RegionName')

echo "Region parameter specified: $REGION"
if [[ ! "$AVAILABLE_REGIONS" == *"$REGION"* ]]; then
    echo "Could not find $REGION in available regions: $AVAILABLE_REGIONS"
    echo ""
    echo "EXITING SCRIPT."
    exit 1
fi

publish_layer() {
    region=$1
    layer_name="dd-trace-java-test"
    aws_version_key=$2
    version_nbr=$(aws lambda publish-layer-version --layer-name $layer_name \
        --description "Datadog Tracer Lambda Layer for Java" \
        --zip-file "fileb://$LAYER_ZIP" \
        --region "$region" \
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

echo "Starting publishing layer for region $REGION..."

publish_layer $REGION "$aws_version_key"
rm $LAYER_ZIP

echo "Done !"
