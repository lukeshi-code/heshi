#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=/opt/heshi-web
BRANCH=
COMMIT_MSG=

cd " \

if ! git remote get-url origin >/dev/null 2>&1; then
 echo ERROR: origin remote is not configured.
 exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
 git add -A
 git commit -m || true
fi

git push origin 

/scripts/deploy_from_github.sh 
