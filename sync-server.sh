#!/usr/bin/env bash

# This script syncs the server with the relevant branch in client

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

SERVER_REPO="test-app"
CLIENT_REPO="client-app"
CLIENT_FOLDER="client"
ORG="test-org-integration"

NEW_REPO_URL=$1
BRANCH=$2
PULL=$3
NEW_REMOTE="remote-${PULL}"
AUTHOR=$4
AUTHOR_EMAIL=$5
TITLE=$6


cd local-repos/${CLIENT_REPO}

# checkout locally the user branch of the client repo
git remote add ${NEW_REMOTE} ${NEW_REPO_URL}
git fetch ${NEW_REMOTE}
git checkout ${NEW_REMOTE}/${BRANCH}

# reset server repo to master
cd ../${SERVER_REPO}
git fetch
git checkout master
git reset --hard origin/master

# create a new branch with the id of the client PR
git checkout -b client-${PULL}

# delete the server version of the client folder
rm -rf ${CLIENT_FOLDER}

# copy over the client version of the client folder
cp -R ../${CLIENT_REPO}/${CLIENT_FOLDER} .

# commit and keep the original author
git add --all
git commit -m "${TITLE}" --author "${AUTHOR} <${AUTHOR_EMAIL}>"
git push origin client-${PULL}

# cleanup the new branch from the local copy of server repo
git checkout master
git branch -D client-${PULL}
git reset --hard origin/master

# cleanup the local copy of the client repo
cd ../${CLIENT_REPO}
git fetch
git checkout master
git reset --hard origin/master
git remote rm ${NEW_REMOTE}
cd ../..