#!/usr/bin/env bash
set -euo pipefail

STAGING_DIR="build/mkdocs-src"

# resolve mkdocs: prefer local venv, fall back to PATH
if [ -x ".venv/bin/mkdocs" ]; then
    MKDOCS=".venv/bin/mkdocs"
elif command -v mkdocs &>/dev/null; then
    MKDOCS="mkdocs"
else
    echo "MkDocs not found. Install it with:"
    echo "  python3 -m venv .venv && .venv/bin/pip install mkdocs"
    exit 1
fi

# prepare staging directory
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

# Copy README.md as index.md.
# README links to docs files using the 'docs/' prefix (e.g., docs/installation.md).
# In the staging flat structure all files are at the same level, so strip that prefix.
sed 's|(\(docs/\)|(|g' README.md > "$STAGING_DIR/index.md"

# copy all docs pages into the staging directory (flat, no subdirectory)
cp docs/*.md "$STAGING_DIR/"

# build the site
$MKDOCS build --config-file mkdocs.yml

echo ""
echo "Documentation built successfully -> build/mkdocs/index.html"
