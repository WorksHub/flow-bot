#!/usr/bin/env bash

# This script syncs the client repo with the latest master version on server

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

SERVER_REPO=$1
CLIENT_REPO=$2
CLIENT_FOLDER=$3

cd local-repos/${SERVER_REPO}
git fetch
HASH=$(git log origin/master -1 --pretty=%h)
MSG=$(git log origin/master -1 --pretty=%s)
git diff origin/master~ origin/master -- ${CLIENT_FOLDER} > ../client-${HASH}.patch
cd ../${CLIENT_REPO}

git fetch
git checkout master
git reset --hard origin/master
git apply ../client-${HASH}.patch
git add --all
git commit -m "${MSG}"
git push origin master
git reset --hard HEAD
rm ../client-${HASH}.patch
cd ../..

