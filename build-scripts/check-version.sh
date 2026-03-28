#!/usr/bin/env bash
set -euo pipefail

PR_VERSION=$(grep '^VERSION_NAME=' gradle-public.properties | cut -d= -f2)
MAIN_VERSION=$(git show origin/main:gradle-public.properties | grep '^VERSION_NAME=' | cut -d= -f2)

echo "PR version:   $PR_VERSION"
echo "Main version: $MAIN_VERSION"

# Validates that $1 matches X.Y.Z or X.Y.Z-{alpha|beta}NN
validate_version() {
    local ver="$1"
    if ! [[ "$ver" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-alpha[0-9]+|-beta[0-9]+)?$ ]]; then
        echo "::error::Invalid VERSION_NAME '$ver'. Expected X.Y.Z or X.Y.Z-{alpha|beta}NN (e.g. 1.0.0 or 1.0.0-beta01)"
        exit 1
    fi
}

# Returns "major minor patch" from a version string (strips pre-release suffix)
semver_parts() {
    local base="${1%%-*}"
    echo "$base" | awk -F. '{ printf "%d %d %d", $1, $2, $3 }'
}

# Returns "type_rank number" for the pre-release part of a version.
# type_rank: 2 = release (no suffix), 1 = beta, 0 = alpha
pre_release_parts() {
    local ver="$1"
    if [[ "$ver" =~ -alpha([0-9]+)$ ]]; then
        echo "0 $((10#${BASH_REMATCH[1]}))"
    elif [[ "$ver" =~ -beta([0-9]+)$ ]]; then
        echo "1 $((10#${BASH_REMATCH[1]}))"
    else
        echo "2 0"
    fi
}

# Returns 0 (true) if $1 > $2 semantically, 1 (false) otherwise.
# Pre-release ordering (ascending): alpha < beta < release
# Within the same pre-release type, higher number wins (alpha02 > alpha01).
semver_gt() {
    read -r maj1 min1 pat1 <<< "$(semver_parts "$1")"
    read -r maj2 min2 pat2 <<< "$(semver_parts "$2")"

    if [ "$maj1" -gt "$maj2" ]; then return 0; fi
    if [ "$maj1" -lt "$maj2" ]; then return 1; fi
    if [ "$min1" -gt "$min2" ]; then return 0; fi
    if [ "$min1" -lt "$min2" ]; then return 1; fi
    if [ "$pat1" -gt "$pat2" ]; then return 0; fi
    if [ "$pat1" -lt "$pat2" ]; then return 1; fi

    # Base X.Y.Z is equal — compare pre-release suffix
    read -r type1 num1 <<< "$(pre_release_parts "$1")"
    read -r type2 num2 <<< "$(pre_release_parts "$2")"

    if [ "$type1" -gt "$type2" ]; then return 0; fi
    if [ "$type1" -lt "$type2" ]; then return 1; fi
    if [ "$num1" -gt "$num2" ]; then return 0; fi
    return 1
}

validate_version "$PR_VERSION"
validate_version "$MAIN_VERSION"

if ! semver_gt "$PR_VERSION" "$MAIN_VERSION"; then
    echo "::error::VERSION_NAME must be greater than the current version on main ($MAIN_VERSION). Got: $PR_VERSION"
    exit 1
fi

echo "Version bumped: $MAIN_VERSION -> $PR_VERSION"
