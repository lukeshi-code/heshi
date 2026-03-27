#!/usr/bin/env bash
set -euo pipefail

REPO_DIR=/opt/heshi-web
BRANCH=

cd " \

echo \[1/4] Fetch latest code from origin/\
git fetch origin \\

echo \[2/4] Reset workspace to origin/\
git checkout \\
git reset --hard \origin/\

echo \[3/4] Build\
mvn -q -DskipTests clean package

echo \[4/4] Restart service\
systemctl restart heshi-web
sleep 3
systemctl --no-pager --full status heshi-web | head -n 20

echo \Deploy finished.\
