#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2019 Datadog, Inc.

# Lists most recent layers ARNs across regions to STDOUT

set -e

AVAILABLE_REGIONS=$(aws ec2 describe-regions --output json | jq -r '.[] | .[] | .RegionName')
LAYERS_MISSING_REGIONS=()

# Check region arg
if [ -z "$2" ]; then
    >&2 echo "Region parameter not specified, running for all available regions."
    REGIONS=$AVAILABLE_REGIONS
else

    >&2 echo "Region parameter specified: $2"
    if [[ ! "$AVAILABLE_REGIONS" == *"$2"* ]]; then
        >&2 echo "Could not find $2 in available regions:" $AVAILABLE_REGIONS
        >&2 echo ""
        >&2 echo "EXITING SCRIPT."
        exit 1
    fi
    REGIONS=($2)
fi


for region in $REGIONS
do
	layer_name="dd-trace-java"
	last_layer_arn=$(aws lambda list-layer-versions --layer-name $layer_name --region $region --output json | jq -r ".LayerVersions | .[0] |  .LayerVersionArn")
	if [ "$last_layer_arn" == "null" ]; then
	    >&2 echo "No layer found for $region, $layer_name"
	    if [[ ! " ${LAYERS_MISSING_REGIONS[@]} " =~ " ${region} " ]]; then
		LAYERS_MISSING_REGIONS+=( $region )
	    fi
	else
	    echo $last_layer_arn
	fi
done

if [ ${#LAYERS_MISSING_REGIONS[@]} -gt 0 ]; then
    echo "WARNING: Following regions missing layers: ${LAYERS_MISSING_REGIONS[@]}"
    exit 1
fi
