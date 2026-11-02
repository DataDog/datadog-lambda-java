#!/bin/bash

# USAGE: download the layer bundle from the build pipeline in gitlab. Use the
# Download button on the `layer` job. This will be a zip file containing the
# layer (there is only 1 layer for the entire Java release). Run this script as follows:
#
# CI_COMMIT_TAG=<vX> ENVIRONMENT=[us1-staging-fed|us1-fed|us2-fed] [REGIONS=us-gov-west-1] ./publish_govcloud.sh <layer-bundle.zip>
# Download layer from the 'build layer' job's release artifacts in Gitlab.
#
# protip: you can drag the zip file from finder into your terminal to insert
# its path.

LAYER_NAME="dd-trace-java"

LAYER_PATH=$1

if [ -z "$LAYER_PATH" ]; then
    printf "[ERROR]: layer path not provided\n"
    exit 1
fi

FILENAME=$(basename "$LAYER_PATH")
if [[ ! "$FILENAME" =~ ^dd-trace-java-[0-9]+\.zip$ ]]; then
    echo "[ERROR]: Unexpected layer path: $LAYER_PATH"
    exit 1
fi

if [ -z "$ENVIRONMENT" ]; then
    printf "[ERROR]: ENVIRONMENT not specified\n"
    exit 1
fi

if [ "$ENVIRONMENT" = "us1-staging-fed" ]; then
    # this role looks like this in ~/.aws/config:
    # [profile sso-govcloud-us1-staging-fed-power-user]
    # sso_start_url=https://start.us-gov-home.awsapps.com/directory/d-9867188aeb
    # sso_account_id=553727695824
    # sso_role_name=power-user
    # sso_region=us-gov-west-1
    # region=us-gov-west-1
    AWS_VAULT_ROLE=sso-govcloud-us1-staging-fed-power-user
    STAGE="gov-staging"

elif [ $ENVIRONMENT = "us1-fed" ]; then
    # this role looks like this in ~/.aws/config:
    # [profile sso-govcloud-us1-fed-engineering]
    # sso_start_url=https://start.us-gov-west-1.us-gov-home.awsapps.com/directory/d-98671fdc8b
    # sso_account_id=002406178527
    # sso_role_name=engineering
    # sso_region=us-gov-west-1
    # region=us-gov-west-1
    AWS_VAULT_ROLE=sso-govcloud-us1-fed-engineering
    STAGE="gov-prod"

elif [ $ENVIRONMENT = "us2-fed" ]; then
    # this role looks like this in ~/.aws/config:
    # [profile sso-govcloud-fed-us2-lambda-layer-operator]
    # sso_start_url=https://start.us-gov-west-1.us-gov-home.awsapps.com/directory/d-98671fdc8b
    # sso_account_id=486696501492
    # sso_role_name=lambda-layer-operator
    # sso_region=us-gov-west-1
    # region=us-gov-west-1
    AWS_VAULT_ROLE=sso-govcloud-fed-us2-lambda-layer-operator
    STAGE="gov-prod"

else
    printf "[ERROR]: ENVIRONMENT not supported, must be us1-staging-fed, us1-fed, or us2-fed.\n"
    exit 1
fi

AWS_VAULT_PREFIX="aws-vault exec $AWS_VAULT_ROLE --"

echo "Checking that you have access to the GovCloud AWS account"
$AWS_VAULT_PREFIX aws sts get-caller-identity

AVAILABLE_REGIONS=$($AWS_VAULT_PREFIX aws ec2 describe-regions | jq -r '.[] | .[] | .RegionName')

# Determine the target regions
if [ -z "$REGIONS" ]; then
    printf "Region not specified, running for all available regions.\n"
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

# Determine the target version from CI_COMMIT_TAG for prod
if [ "$STAGE" == "gov-prod" ]; then
    if [ -z "$CI_COMMIT_TAG" ]; then
        printf "[ERROR]: No CI_COMMIT_TAG found.\n"
        printf "Exiting script...\n"
        exit 1
    else
        printf "Tag found in environment: $CI_COMMIT_TAG\n"
    fi

    VERSION="${CI_COMMIT_TAG//[!0-9]/}"
    printf "Version: ${VERSION}\n"

    if [ -z "$VERSION" ]; then
        printf "[ERROR]: Layer VERSION could not be parsed from CI_COMMIT_TAG\n"
        exit 1
    else
        printf "Layer version parsed: $VERSION\n"
    fi
fi

publish_layer() {
    region=$1
    NEW_VERSION=$($AWS_VAULT_PREFIX aws lambda publish-layer-version --layer-name $LAYER_NAME \
        --description "Datadog Tracer Lambda Layer for Java" \
        --compatible-runtimes "java11" "java17" "java21" "java25" \
        --compatible-architectures "x86_64" "arm64" \
        --zip-file "fileb://$LAYER_PATH" \
        --region $region \
        | jq -r '.Version')

    if [ "$STAGE" == "gov-prod" ]; then
        permission=$($AWS_VAULT_PREFIX aws lambda add-layer-version-permission --layer-name $LAYER_NAME \
            --version-number $NEW_VERSION \
            --statement-id "release-$NEW_VERSION" \
            --action lambda:GetLayerVersion \
            --principal "*" \
            --region $region
        )
    fi

    echo $NEW_VERSION
}

for region in $REGIONS
do
    echo "Publishing tracer layer to region $region in stage $STAGE."

    latest_version=$($AWS_VAULT_PREFIX aws lambda list-layer-versions --region $region --layer-name $LAYER_NAME --query 'LayerVersions[0].Version || `0`')

    if [ "$STAGE" == "gov-staging" ]; then
        TARGET_VERSION=$(($latest_version + 1))
    else
        TARGET_VERSION=$VERSION
        if [ $latest_version -ge $TARGET_VERSION ]; then
            printf "[$region] Layer $LAYER_NAME version $TARGET_VERSION already exists in region $region, skipping...\n"
            continue
        elif [ $latest_version -lt $((TARGET_VERSION-1)) ]; then
            printf "[$region][WARNING] The latest version of layer $LAYER_NAME in region $region is $latest_version, this will publish all the missing versions including $TARGET_VERSION\n"
        fi
    fi

    while [ $latest_version -lt $TARGET_VERSION ]; do
        latest_version=$(publish_layer $region)
        printf "[$region] Published version $latest_version of layer $LAYER_NAME in region $region\n"

        # This shouldn't happen unless someone manually deleted the latest version, say 28, and
        # then tries to republish 28 again. The published version would actually be 29, because
        # Lambda layers are immutable and AWS will skip deleted version and use the next number.
        if [ $latest_version -gt $TARGET_VERSION ]; then
            printf "[$region] Published version $latest_version is greater than the desired version $TARGET_VERSION!"
            exit 1
        fi
    done

    printf "[$region] Finished publishing layer...\n\n"
done

echo "Done!"
