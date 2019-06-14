#!/usr/bin/env bash

# This script initializes the local repositories under the folder
# `local-repos`. It also sets the Git user for WorksHubCodi.

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

SERVER_REPO=$1
SERVER_GIT=$2
CLIENT_REPO=$3
CLIENT_GIT=$4
GIT_USER=$5
GIT_EMAIL=$6

mkdir -p local-repos

cd local-repos

rm -rf *
git clone ${SERVER_GIT}
git clone ${CLIENT_GIT}

cd ${SERVER_REPO}

git config user.name ${GIT_USER}
git config user.email ${GIT_EMAIL}

cd ../${CLIENT_REPO}

git config user.name ${GIT_USER}
git config user.email ${GIT_EMAIL}

cd ../..
