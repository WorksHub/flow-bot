#!/usr/bin/env bash


# This script syncs the server with the relevant branch in client
# Args: $org $repo $branch $pull

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
git fetch
git checkout master
git reset --hard origin/master
git remote add ${NEW_REMOTE} ${NEW_REPO_URL}
git fetch ${NEW_REMOTE}
git diff origin/master ${NEW_REMOTE}/${BRANCH} -- ${CLIENT_FOLDER} > ../server-${PULL}.patch
git remote rm ${NEW_REMOTE}
cd ../${SERVER_REPO}
git fetch
git checkout master
git reset --hard origin/master
git checkout -b client-${PULL}
git apply ../server-${PULL}.patch
git add --all
git commit -m "${TITLE}" --author "${AUTHOR} <${AUTHOR_EMAIL}>"
git push origin client-${PULL}
git checkout master
git branch -D client-${PULL}
git reset --hard origin/master
rm ../server-${PULL}.patch
cd ../..