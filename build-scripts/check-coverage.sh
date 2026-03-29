#!/usr/bin/env bash
set -euo pipefail

# Usage: bash build-scripts/check-coverage.sh [threshold]
# threshold: minimum required instruction coverage percentage (default: 80)
#
# Reads the JaCoCo XML report for lib-compiler, prints the coverage percentage,
# writes it to the GitHub Actions job summary (if running in CI), and exits with
# a non-zero status if coverage is below the threshold.

THRESHOLD="${1:-80}"

# shellcheck source=jacoco-coverage.sh
source "$(dirname "$0")/jacoco-coverage.sh"

echo "Coverage: ${PCT}% (threshold: ${THRESHOLD}%)"

# Write to GitHub Actions job summary when running in CI.
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    if [ "$PCT_INT" -ge "$THRESHOLD" ]; then
        STATUS=":white_check_mark:"
    else
        STATUS=":x:"
    fi
    echo "### ${STATUS} Test Coverage: ${PCT}% (threshold: ${THRESHOLD}%)" >> "$GITHUB_STEP_SUMMARY"
fi

if awk "BEGIN { exit ($PCT < $THRESHOLD) ? 0 : 1 }"; then
    echo "::error::Coverage ${PCT}% is below the required ${THRESHOLD}% threshold."
    exit 1
fi
