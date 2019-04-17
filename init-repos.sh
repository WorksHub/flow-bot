#!/usr/bin/env bash

## This file ensures the server and client repo exist

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

## TODO what todo with this?
SERVER_REPO="test-app"
SERVER_GIT="https://github.com/test-org-integration/test-app"

CLIENT_REPO="client-app"
CLIENT_GIT="https://github.com/test-org-integration/client-app"
mkdir -p local-repos

cd local-repos

if [[ ! -f ${SERVER_REPO} ]]; then
    git clone ${SERVER_GIT}
fi

if [[ ! -f ${CLIENT_REPO} ]]; then
    git clone ${CLIENT_GIT}
fi
cd ..
