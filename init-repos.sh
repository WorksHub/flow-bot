#!/usr/bin/env bash

## This file ensures the server and client repo exist

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

SERVER_REPO=$1
SERVER_GIT=$2
CLIENT_REPO=$3
CLIENT_GIT=$4

mkdir -p local-repos

cd local-repos

rm -rf *
git clone ${SERVER_GIT}
git clone ${CLIENT_GIT}

git config --global user.name "WorksHubCodi"
git config --global user.email "codi@works-hub.com"

cd ..
