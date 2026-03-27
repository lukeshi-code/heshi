#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=/opt/heshi-web
BRANCH=${1:-main}

cd "$REPO_DIR"

echo "[1/4] Fetch latest code from origin/$BRANCH"
git fetch origin "$BRANCH"

echo "[2/4] Reset workspace to origin/$BRANCH"
git checkout "$BRANCH"
git reset --hard "origin/$BRANCH"

echo "[3/4] Build"
mvn -q -DskipTests clean package

echo "[4/4] Restart service"
systemctl restart heshi-web
sleep 3
systemctl --no-pager --full status heshi-web | head -n 20

echo "Deploy finished."