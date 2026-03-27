#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=/opt/heshi-web
BRANCH=${1:-main}
COMMIT_MSG=${2:-"chore: update website"}

cd "$REPO_DIR"

if ! git remote get-url origin >/dev/null 2>&1; then
  echo "ERROR: origin remote is not configured."
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  git add -A
  git commit -m "$COMMIT_MSG" || true
fi

git push origin "$BRANCH"
"$REPO_DIR/scripts/deploy_from_github.sh" "$BRANCH"