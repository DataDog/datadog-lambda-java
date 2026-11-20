#!/bin/bash

# Unless explicitly stated otherwise all files in this repository are licensed
# under the Apache License Version 2.0.
# This product includes software developed at Datadog (https://www.datadoghq.com/).
# Copyright 2026 Datadog, Inc.

# Generates the GitHub release notes for a new dd-trace-java layer release, in the
# same format as https://github.com/DataDog/datadog-lambda-java/releases/tag/v30.
#
# USAGE:
#   ./scripts/generate_release_notes.sh <layer-zip> <version>
#
#   <layer-zip>  The layer zip (dd-trace-java-<pipeline-id>.zip), or the artifacts.zip
#                downloaded from the `sign layer` job in GitLab that wraps it.
#   <version>    The release being cut, e.g. v31.
#
# The dd-trace-java version is read from the dd-java-agent.jar inside the layer, and
# the previous one from the "dd-trace-java version" line of the latest GitHub release.
# "What's changed" lists the "AWS Lambda instrumentation" entries from every
# dd-trace-java release in between.
#
# The notes go to stdout, so you can review them and create a draft release:
#   ./scripts/generate_release_notes.sh artifacts.zip v31 > notes.md
#   gh release create v31 --draft --title v31 --notes-file notes.md
#
# Requires: gh (logged in), jq, unzip.

set -euo pipefail

REPO="DataDog/datadog-lambda-java"
TRACER_REPO="DataDog/dd-trace-java"
LAYER_NAME="dd-trace-java"
AWS_ACCOUNT="464622532012"
AWS_GOV_ACCOUNT="002406178527"
AGENT_JAR_PATH="java/lib/dd-java-agent.jar"

err() { printf '[ERROR]: %s\n' "$*" >&2; exit 1; }
warn() { printf '[WARNING]: %s\n' "$*" >&2; }
info() { printf '%s\n' "$*" >&2; }

if [ $# -ne 2 ]; then
    info "Usage: $0 <layer-zip> <version>    e.g. $0 artifacts.zip v31"
    exit 1
fi

LAYER_ZIP=$1
if [[ ! "$2" =~ ^v?([0-9]+)$ ]]; then
    err "Version must look like v31, got: $2"
fi
VERSION=$((10#${BASH_REMATCH[1]}))

for cmd in gh jq unzip; do
    command -v "$cmd" >/dev/null || err "$cmd is required but not installed"
done

[ -f "$LAYER_ZIP" ] || err "Layer zip not found: $LAYER_ZIP"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# --- dd-trace-java version in the new layer ---------------------------------------

LAYER="$LAYER_ZIP"
if ! unzip -l "$LAYER" "$AGENT_JAR_PATH" >/dev/null 2>&1; then
    # Not the layer itself; maybe the GitLab artifacts.zip that wraps it.
    INNER=$(unzip -Z1 "$LAYER_ZIP" 2>/dev/null | grep -E '^\.layers/dd-trace-java-[0-9]+\.zip$' | head -n 1 || true)
    [ -n "$INNER" ] || err "$LAYER_ZIP contains neither $AGENT_JAR_PATH nor .layers/dd-trace-java-<id>.zip"
    unzip -q -j "$LAYER_ZIP" "$INNER" -d "$WORK_DIR"
    LAYER="$WORK_DIR/$(basename "$INNER")"
    unzip -l "$LAYER" "$AGENT_JAR_PATH" >/dev/null 2>&1 || err "$INNER inside $LAYER_ZIP does not contain $AGENT_JAR_PATH"
fi

unzip -q -j "$LAYER" "$AGENT_JAR_PATH" -d "$WORK_DIR"
NEW_TRACER=$(unzip -p "$WORK_DIR/dd-java-agent.jar" META-INF/MANIFEST.MF | tr -d '\r' | sed -n 's/^Implementation-Version: *//p')
if [[ ! "$NEW_TRACER" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    err "Unexpected dd-java-agent Implementation-Version in the layer: '$NEW_TRACER'"
fi
info "Layer contains dd-trace-java v$NEW_TRACER"

# --- dd-trace-java version in the previous release ---------------------------------

LATEST_JSON=$(gh release view --repo "$REPO" --json tagName,body) || err "Could not fetch the latest release of $REPO"
LATEST_TAG=$(jq -r '.tagName' <<<"$LATEST_JSON")
if [[ ! "$LATEST_TAG" =~ ^v([0-9]+)$ ]]; then
    err "Unexpected tag on the latest $REPO release: $LATEST_TAG"
fi
LATEST_NUM=$((10#${BASH_REMATCH[1]}))
if [ "$LATEST_NUM" -ge "$VERSION" ]; then
    err "The latest $REPO release is already $LATEST_TAG, so v$VERSION is not a new release"
elif [ "$LATEST_NUM" -ne $((VERSION - 1)) ]; then
    warn "The latest $REPO release is $LATEST_TAG, not v$((VERSION - 1)); comparing against it anyway"
fi

PREV_TRACER=$(jq -r '.body' <<<"$LATEST_JSON" | tr -d '\r' \
    | sed -nE 's/.*dd-trace-java version \[v?([0-9]+\.[0-9]+\.[0-9]+)\].*/\1/p' | head -n 1)
[ -n "$PREV_TRACER" ] || err "Could not find a 'dd-trace-java version [vX.Y.Z]' line in the $LATEST_TAG release notes"
info "Previous release $LATEST_TAG used dd-trace-java v$PREV_TRACER"

# --- AWS Lambda changes from the dd-trace-java releases in between -----------------

TRACER_RELEASES=$(gh api "repos/$TRACER_REPO/releases?per_page=100") || err "Could not fetch $TRACER_REPO releases"

# Releases newer than PREV_TRACER and up to NEW_TRACER, oldest first.
JQ_VER='def ver: ltrimstr("v") | split(".") | map(tonumber);
    [ .[] | select((.draft or .prerelease) | not)
      | select(.tag_name | test("^v[0-9]+\\.[0-9]+\\.[0-9]+$")) ]'

jq -e --arg v "v$NEW_TRACER" 'any(.[]; .tag_name == $v)' <<<"$TRACER_RELEASES" >/dev/null \
    || err "dd-trace-java v$NEW_TRACER is not among the latest 100 $TRACER_REPO releases on GitHub"

LATEST_TRACER=$(jq -r "$JQ_VER | max_by(.tag_name | ver) | .tag_name" <<<"$TRACER_RELEASES")
if [ "$LATEST_TRACER" != "v$NEW_TRACER" ]; then
    warn "A newer dd-trace-java ($LATEST_TRACER) is out; the layer contains v$NEW_TRACER"
fi

OLDEST_TRACER=$(jq -r "$JQ_VER | min_by(.tag_name | ver) | .tag_name | ltrimstr(\"v\")" <<<"$TRACER_RELEASES")
if jq -en --arg a "$OLDEST_TRACER" --arg b "$PREV_TRACER" 'def ver: split(".") | map(tonumber); ($a | ver) > ($b | ver)' >/dev/null; then
    warn "v$PREV_TRACER is older than the latest 100 $TRACER_REPO releases; What's changed may be incomplete"
fi

case $(jq -rn --arg a "$NEW_TRACER" --arg b "$PREV_TRACER" \
    'def ver: split(".") | map(tonumber); ($a | ver) as $x | ($b | ver) as $y | if $x > $y then "newer" elif $x == $y then "same" else "older" end') in
    older) err "The layer's dd-trace-java v$NEW_TRACER is older than v$PREV_TRACER from $LATEST_TAG" ;;
    same) warn "The layer's dd-trace-java v$NEW_TRACER is the same version as in $LATEST_TAG" ;;
esac

IN_RANGE=$(jq --arg prev "$PREV_TRACER" --arg new "$NEW_TRACER" \
    "$JQ_VER | map(select((.tag_name | ver) > (\$prev | ver) and (.tag_name | ver) <= (\$new | ver))) | sort_by(.tag_name | ver)" \
    <<<"$TRACER_RELEASES")
info "Collecting AWS Lambda changes from: $(jq -r 'map(.tag_name) | join(", ")' <<<"$IN_RANGE")"

# Print the bullets under each "## AWS Lambda instrumentation" heading as single lines
# (an entry can wrap onto the next line), then rewrite them like the v30 notes:
# emoji instead of :shortcodes:, and #123 as a full dd-trace-java PR link.
CHANGES=$(jq -r '.[] | (.body | gsub("\r"; "")), "# end of release"' <<<"$IN_RANGE" \
    | awk '
        function flush() { if (entry != "") print entry; entry = "" }
        /^#/ { flush(); in_lambda = (tolower($0) ~ /^## +aws lambda/); next }
        !in_lambda { next }
        /^\* / { flush(); entry = substr($0, 3); next }
        /^[[:space:]]*$/ { flush(); next }
        entry != "" { sub(/^[[:space:]]+/, ""); entry = entry " " $0 }
        END { flush() }' \
    | sed -E \
        -e 's/:bug:/🐛/g' \
        -e 's/:sparkles:/✨/g' \
        -e 's/:zap:/⚡/g' \
        -e 's/:broom:/🧹/g' \
        -e 's/:bulb:/💡/g' \
        -e 's/:book:/📖/g' \
        -e 's/:closed_lock_with_key:/🔐/g' \
        -e "s,([ (])#([0-9]+),\\1https://github.com/$TRACER_REPO/pull/\\2,g" \
    | awk '!seen[$0]++')

if [ -z "$CHANGES" ]; then
    warn "No AWS Lambda instrumentation entries found; leaving out the What's changed section"
fi

# --- Release notes -------------------------------------------------------------------

FENCE='```'

cat <<EOF
>[!IMPORTANT]
> dd-trace-java version [v$NEW_TRACER](https://github.com/$TRACER_REPO/releases/tag/v$NEW_TRACER)

EOF

if [ -n "$CHANGES" ]; then
    cat <<EOF
## What's changed
$CHANGES

EOF
fi

cat <<EOF
## dd-trace-java layer ARN:

### AWS Regions
$FENCE
arn:aws:lambda:<AWS_REGION>:$AWS_ACCOUNT:layer:$LAYER_NAME:$VERSION
$FENCE

### AWS gov regions
$FENCE
arn:aws-us-gov:lambda:<AWS_GOV_REGION>:$AWS_GOV_ACCOUNT:layer:$LAYER_NAME:$VERSION
$FENCE
EOF

if [ "$NEW_TRACER" != "$PREV_TRACER" ]; then
    printf '\n**Full Changelog**: https://github.com/%s/compare/v%s...v%s\n' "$TRACER_REPO" "$PREV_TRACER" "$NEW_TRACER"
fi
