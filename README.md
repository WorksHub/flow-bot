# flow-bot

This is a clojure application designed to keep in sync a **private** repository (defined in the code as `server-repo`) and a **public** repository (defined in the code as `client-repo`). Both repositories need to be on GitHub.

As of now this syncs **one shared folder** (defined as `client-folder` in the code) between `client-repo` and `server-repo`.

## Rationale

This application enables a _sync-based workflow_ for open-sourcing only one part (or one folder) of a larger repository. It is built to satisfy a few requirements:

- Updates to `client-folder` in `server-repo` should propagate to `client-repo`
- People can open Pull Requests against `client-repo`
- Once deemed **OK** to be merged, changes are brought into `server-repo`
- Attribution is kept consistent in both repositories, as much as possible.

## Usage:

### Requirements
- One github account (with `auth` token) to use as "actor" for all the synchronization. (TODO: Use GitHub Apps)
- A `client-repo`
- A `server-repo`


### Setup
1. Configure the environment variables (as documented at the bottom of this README)
2. Run `flow-bot` in a server somewhere, nothing down the address and the port
3. On GitHub, go to `client-repo` > Settings > Webhooks and click on "Add webhook" 
4. Enter the URL of your instance of `flow-bot` in "Paylod URL"
5. Set `Content-type` to `application/json`
6. Select "Send me **everything**"
7. Ensure the "Active" checkbox is checked
8. Click on "Add webhook"
9. Repeat steps 3-8 for the `server-repo` 


## High level overview:

### Syncing client to server

**Whenever there is an open PR on `client-repo`**:

- The application waits for a **PR comment** that contains the `magic-string` (in our example `OK TO MERGE`)
- The application checks out the PR branch on `client-repo`
- The application then checks out `master` on `server-repo`, creates a new branch called `client-$i` where `$i` is the PR number on GitHub 
- The application copies over the `client` folder from `client-repo` to on `server-repo`
- The application commits the changes to `server-repo` attributing the commit authorship to the original committer of the PR
- The application creates a new PR on `server-repo`


**Whenever a PR that originated from  `client-repo` gets merged in `server-repo`**:

- The application keeps track of the original author in local app-state
- The application comments over the `client-repo` PR, informing the original committer that their PR was finally merged upstream
- The application tries to close the PR on `client-repo` (**Note**: this is currently not working, GitHub does respond to our API call but the PR is not closed, requiring manual intervention)

### Syncing server to client

**Whenever there is a new commit on `master` branch on `server-repo`**:

- The application does a checkout of `master` on `client-repo` and **copies over** the `client` folder from `server-repo` to `client-repo`
- The application commits the changes to `client-repo`, giving correct attribution.
- The commit is attributed to the author of the first commit in the merged PR. In this way, if the changes was originally from `client-repo`, we attribute the authorship to the correct contributor.

## Running it locally

The program needs the following environment variables, supplied, for example, with a `.lein-env` file

```clojure
{:server-org    "technomancy"
 :server-repo   "leiningen"
 :server-git    "https://USER:TOKEN@github.com/technomancy/leiningen.git"
 :client-org    "WorksHub"
 :client-repo   "client"
 :client-git    "https://USER:TOKEN@github.com/WorksHub/client.git"
 :client-folder "client"
 :auth          "TOKEN"
 :magic-string  "OK TO MERGE"
 :port          "3000"}
```

Note that `:server-git` and `:client-git` environment variable contain the token from your chosen user in the repo URL.