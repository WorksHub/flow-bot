#!/usr/bin/env bash

# This script syncs the server with the relevant branch in client

set -x
set -e # Exit with nonzero when a command fails.
set -u # Exit with nonzero when referencing an unbound variable

NEW_REPO_URL=$1
BRANCH=$2
PULL=$3
NEW_REMOTE="remote-${PULL}"
NEW_CLIENT="client-${PULL}"
AUTHOR=$4
AUTHOR_EMAIL=$5
TITLE=$6
SERVER_REPO=$7
CLIENT_REPO=$8
CLIENT_FOLDER=$9

cd local-repos/${CLIENT_REPO}

# clean up existing remotes
git remote | grep -v origin | xargs -r -n 1 git remote rm

# sync up local master branch
git fetch
git checkout master
git reset --hard origin/master

# checkout locally the user branch of the client repo
git remote add ${NEW_REMOTE} ${NEW_REPO_URL}
git fetch ${NEW_REMOTE}
git checkout ${NEW_REMOTE}/${BRANCH}

# merge master with the local branch
git merge master

# reset server repo to master
cd ../${SERVER_REPO}
git fetch
git checkout master
git reset --hard origin/master

# create a new branch with the id of the client PR
git checkout -b ${NEW_CLIENT}

# delete the content of the server version of the client folder
rm -rf ${CLIENT_FOLDER}/*

# copy over the files from the client repo
cp -R ../${CLIENT_REPO}/* ${CLIENT_FOLDER}/
#rm -rf ${CLIENT_FOLDER}/.git

# commit and keep the original author
git add --all
git commit -m "${TITLE}" --author "${AUTHOR} <${AUTHOR_EMAIL}>"
# if git ls-remote --heads origin | grep ${NEW_CLIENT}; then
#     echo "Found existing branch so pulling..."
#     git pull origin client-${PULL} --no-edit
# else
#     echo "No existing branch so continuing..."
# fi

git push origin ${NEW_CLIENT}

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
