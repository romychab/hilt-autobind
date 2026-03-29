#!/usr/bin/env bash
set -euo pipefail

# Reads the JaCoCo XML report for lib-compiler and updates the coverage badge
# by writing a Shields.io endpoint JSON to a GitHub Gist.
#
# Required environment variables:
#   GIST_TOKEN  - GitHub personal access token with the 'gist' scope
#   GIST_ID     - ID of the Gist containing hilt-autobind-coverage.json

# shellcheck source=jacoco-coverage.sh
source "$(dirname "$0")/jacoco-coverage.sh"

if [ "$PCT_INT" -ge 80 ]; then COLOR="brightgreen"
elif [ "$PCT_INT" -ge 60 ]; then COLOR="yellow"
elif [ "$PCT_INT" -ge 40 ]; then COLOR="orange"
else COLOR="red"
fi

echo "Updating coverage badge: ${PCT_INT}% (${COLOR})"

COVERAGE_JSON=$(printf '{"schemaVersion":1,"label":"Coverage","message":"%s%%","color":"%s"}' "$PCT_INT" "$COLOR")
PAYLOAD=$(jq -n --arg content "$COVERAGE_JSON" \
    '{"files":{"hilt-autobind-coverage.json":{"content":$content}}}')

curl -s -X PATCH \
    -H "Authorization: token $GIST_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" \
    "https://api.github.com/gists/$GIST_ID"
