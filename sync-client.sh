#!/usr/bin/env bash

# This script syncs the client repo with the latest master version on server

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

SERVER_REPO=$1
CLIENT_REPO=$2
CLIENT_FOLDER=$3
MSG=$4
AUTHOR_NAME=$5
AUTHOR_EMAIL=$6

cd local-repos/${SERVER_REPO}
git fetch
# HASH=$(git log origin/master -1 --pretty=%h)
# MSG=$(git log origin/master -1 --pretty=%s)
git checkout master
git reset --hard origin/master
cd ../${CLIENT_REPO}

git fetch
git checkout master
git reset --hard origin/master
rm -rf ${CLIENT_FOLDER}
cp -R ../${SERVER_REPO}/${CLIENT_FOLDER} .
git add --all
git commit -m "${MSG}" --author "${AUTHOR_NAME} <${AUTHOR_EMAIL}>"
git push origin master
git reset --hard HEAD
cd ../..

