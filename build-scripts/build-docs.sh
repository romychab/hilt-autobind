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
sed '/<!-- docs-exclude-start -->/,/<!-- docs-exclude-end -->/d' README.md \
    | sed 's|(\(docs/\)|(|g' > "$STAGING_DIR/index.md"

# copy all docs pages into the staging directory (flat, no subdirectory)
cp docs/*.md "$STAGING_DIR/"

# Convert <!-- block-start: {type} --> ... <!-- block-end --> to MkDocs admonitions
python3 - "$STAGING_DIR" <<'EOF'
import re, sys
from pathlib import Path

def transform(m):
    block_type = m.group(1)
    body = m.group(2).strip('\n')
    indented = '\n'.join('    ' + line for line in body.split('\n'))
    return f'!!! {block_type}\n{indented}'

for path in Path(sys.argv[1]).glob('*.md'):
    content = path.read_text()
    updated = re.sub(
        r'<!-- block-start: (\S+) -->\n(.*?)<!-- block-end -->',
        transform,
        content,
        flags=re.DOTALL,
    )
    if updated != content:
        path.write_text(updated)
EOF

# build the site
$MKDOCS build --config-file mkdocs.yml

echo ""
echo "Documentation built successfully -> build/mkdocs/index.html"
