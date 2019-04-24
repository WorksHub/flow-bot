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

# resets local copy of server repo to latest master
cd local-repos/${SERVER_REPO}
git fetch
git checkout master
git reset --hard origin/master

# resets local copy of client repo to latest master
cd ../${CLIENT_REPO}
git fetch
git checkout master
git reset --hard origin/master
rm -rf ${CLIENT_FOLDER}

# copies over the client folder from server to client
cp -R ../${SERVER_REPO}/${CLIENT_FOLDER} .
git add --all

# attribute the commit to the original author on the server repo
git commit -m "${MSG}" --author "${AUTHOR_NAME} <${AUTHOR_EMAIL}>"
git push origin master

# clean up and go back to top level
git reset --hard HEAD
cd ../..

