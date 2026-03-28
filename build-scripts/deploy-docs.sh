#!/usr/bin/env bash
set -euo pipefail

# Required environment variables:
#   DEPLOY_SSH_KEY - private SSH key content
#   DEPLOY_HOST_KEY - known host entry
#   DEPLOY_HOST - remote server hostname or IP
#   DEPLOY_USER - remote user to connect as
#   DEPLOY_DOC_PATH - remote path where the docs must be placed

mkdir -p ~/.ssh

printf '%s\n' "$DEPLOY_SSH_KEY" | install -m 600 /dev/stdin ~/.ssh/id_ed25519
trap 'rm -f ~/.ssh/id_ed25519' EXIT

if ! grep -qF "$DEPLOY_HOST_KEY" ~/.ssh/known_hosts 2>/dev/null; then
    echo "$DEPLOY_HOST_KEY" >> ~/.ssh/known_hosts
fi

if [ ! -d "build/mkdocs" ]; then
    echo "Error: build/mkdocs directory not found. Run build-scripts/build-docs.sh first." >&2
    exit 1
fi

rsync -az --delete --timeout=60 build/mkdocs/ "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DOC_PATH"

rm -f ~/.ssh/id_ed25519
