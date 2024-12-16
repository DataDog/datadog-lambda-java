#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2024 Datadog, Inc.

set -e

LAYER_NAME="dd-trace-java"

publish_layer() {
    region=$1

    version_nbr=$(aws lambda publish-layer-version --layer-name $LAYER_NAME \
        --description "Datadog Tracer Lambda Layer for Java" \
        --compatible-runtimes "java8" "java11" "java17" "java21" \
        --compatible-architectures "x86_64" "arm64" \
        --zip-file "fileb://.layers/tracer.zip" \
        --region $region \
        | jq -r '.Version')

    # Add permissions only for prod
    if [ "$STAGE" == "prod" ]; then
        permission=$(aws lambda add-layer-version-permission --layer-name $LAYER_NAME \
            --version-number $version_nbr \
            --statement-id "release-$version_nbr" \
            --action lambda:GetLayerVersion \
            --principal "*" \
            --region $region
        )
    fi

    echo $version_nbr
}


if [ ! -f ".layers/tracer.zip" ]; then
    printf "[ERROR]: Could not find .layers/tracer.zip."
    exit 1
fi

AVAILABLE_REGIONS=$(aws ec2 describe-regions | jq -r '.[] | .[] | .RegionName')

if [ -z "$REGION" ]; then
    printf "[ERROR]: REGION not specified."
    exit 1
else
    printf "Region specified: $REGION\n"
    if [[ ! "$AVAILABLE_REGIONS" == *"$REGION"* ]]; then
        printf "Could not find $REGION in available regions: $AVAILABLE_REGIONS"
        exit 1
    fi
fi

if [ -z "$STAGE" ]; then
    printf "[ERROR]: STAGE not specified.\n"
    exit 1
fi

printf "[$REGION] Starting publishing layers...\n"

if [[ "$STAGE" =~ ^(staging|sandbox)$ ]]; then
    # Deploy latest version
    latest_version=$(aws lambda list-layer-versions --region $REGION --layer-name $LAYER_NAME --query 'LayerVersions[0].Version || `0`')
    VERSION=$(($latest_version + 1))
else
    # Running on prod
    if [ -z "$CI_COMMIT_TAG" ]; then
        printf "[ERROR]: No CI_COMMIT_TAG found.\n"
        printf "Exiting script...\n"
        exit 1
    else
        printf "Tag found in environment: $CI_COMMIT_TAG\n"
    fi

    VERSION="${CI_COMMIT_TAG//[!0-9]/}"
    printf "Version: ${VERSION}\n"
fi

if [ -z "$VERSION" ]; then
    printf "[ERROR]: Layer VERSION not specified"
    exit 1
else
    printf "Layer version parsed: $VERSION\n"
fi

latest_version=$(aws lambda list-layer-versions --region $REGION --layer-name $LAYER_NAME --query 'LayerVersions[0].Version || `0`')
if [ $latest_version -ge $VERSION ]; then
    printf "[$REGION] Layer $LAYER_NAME version $VERSION already exists in region $REGION, skipping...\n"
    exit 1
elif [ $latest_version -lt $((VERSION-1)) ]; then
    printf "[$REGION][WARNING] The latest version of layer $LAYER_NAME in region $REGION is $latest_version, this will publish all the missing versions including $VERSION\n"
fi

while [ $latest_version -lt $VERSION ]; do
    latest_version=$(publish_layer $REGION)
    printf "[$REGION] Published version $latest_version of layer $LAYER_NAME in region $REGION\n"

    # This shouldn't happen unless someone manually deleted the latest version, say 28, and
    # then tries to republish 28 again. The published version would actually be 29, because
    # Lambda layers are immutable and AWS will skip deleted version and use the next number.
    if [ $latest_version -gt $VERSION ]; then
        printf "[$REGION] Published version $latest_version is greater than the desired version $VERSION!"
        exit 1
    fi
done

printf "[$REGION] Finished publishing layer...\n\n"
