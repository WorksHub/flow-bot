# Flow-bot

<img src="resources/robotic-arm.svg" alt="Pick and place between repos" width="200px" align="right">

> Butter Robot: "What is my purpose?"
> 
> [Rick Sanchez](https://en.wikipedia.org/wiki/Rick_Sanchez_(Rick_and_Morty)): "You pass butter".


This is a Clojure application designed to keep in sync a **main repository** and a **secondary repository** which is a sub-part (most commonly a folder) of the first, similarly to a git submodule, but with none of the hassle.

In fact, with this bot you can keep working on your main repository with the same workflow, but can still separate a part of it (and maybe open source it).

This is especially useful in case of monorepos.

## Rationale

At WorksHub, we use a single repository for our application, which consists of a Clojurescript frontend and Clojure backend. We recently wanted to open source our [frontend](https://github.com/WorksHub/client) and tried to use `git submodules`. This resulted in a very clunky workflow, because often features and requests spanned both frontend and backend, so we needed to open PRs in two separate repositories, and syncing them was not very easy (especially the pinning of the version).

From these difficulties, we changed approach and gravitated towards a _sync-based workflow_ that satisfies the following criteria:

- Two separate repositories (one private, with backend and frontend, and one public with frontend code only)
- Our workflow is impacted as little as possible (i.e. the bot should work mostly _automatically_)
- Changes from one repo are propagated in the other, and authorship is kept intact

## Usage:

### Requirements
- A main repository (defined from now on as `server-repo`) on GitHub
- A secondary repository (defined from now on as `client-repo`) on GitHub
- One GitHub account (with `auth` token) to use as "actor" for all the synchronization (therefore it needs write access to both repositories).

### Setup
1. Configure the environment variables (as documented at the bottom of this README)
2. Run `flow-bot` in a server somewhere
3. On GitHub, go to `client-repo` > Settings > Webhooks and click on "Add webhook" 
4. Enter the URL (and the port, if necessary) of your instance of `flow-bot` in "Paylod URL"
5. Set `Content-type` to `application/json`
6. Select "Send me **everything**"
7. Ensure the "Active" checkbox is checked
8. Click on "Add webhook"
9. Repeat steps 3-8 for the `server-repo` 


## High level overview:

### Syncing client to server (changes going upstream)

**Whenever there is an open PR on `client-repo`**:

- The application waits for a **PR comment** that contains the `magic-string` (in our example **`OK TO MERGE`**)
- The application checks out the PR branch on `client-repo`
- The application then checks out `master` on `server-repo`, creates a new branch called `client-$i` where `$i` is the PR number on GitHub 
- The application copies over the `client` folder from `client-repo` to on `server-repo`
- The application commits the changes to `server-repo` attributing the commit authorship to the original committer of the PR
- The application creates a new PR on `server-repo`


**Whenever a PR that originated from  `client-repo` gets merged in `server-repo`**:

- The application keeps track of the original author in local app-state
- The application comments over the `client-repo` PR, informing the original committer that their PR was finally merged upstream
- The application tries to close the PR on `client-repo` (**Note**: this is currently not working, GitHub does respond to our API call but the PR is not closed, requiring manual intervention)

### Syncing server to client (changes going downstream)

**Whenever there is a new commit on `master` branch on `server-repo`**:

- The application does a checkout of `master` on `client-repo` and **copies over** the `client` folder from `server-repo` to `client-repo`
- The application commits the changes to `client-repo`, giving correct attribution.
- The commit is attributed to the author of the first commit in the merged PR. In this way, if the changes was originally from `client-repo`, we attribute the authorship to the correct contributor.

## Running it locally

### Prerequisites

This program uses GNU version of `xargs`. If you want to hack on flow-bot are running under macOS, make sure to execute

```shell
brew install findutils
```

and follow the instructions on screen to add the GNU versions of those programs on your machine.

This is necessary because the version of xargs that comes shipped with macOS does not support the `-r` argument used by the script

### Environment variables for the bot

The program needs the following environment variables, supplied, for example, with a `.lein-env` file

```clojure
{:server-org    "test-org-integration"
 :server-repo   "app"
 :client-org    "test-org-integration"
 :client-repo   "client"
 :client-folder "client"
 :magic-string  "OK TO MERGE"
 :git-user      "GitUser"
 :git-email     "gitemail@example.com"
 :git-token     "SUPERSECRETTOKEN"
 :port          "3000"}
```

### Credits

Icons made by [Freepik](https://www.freepik.com/) from [Flaticon](https://www.flaticon.com/), licensed by [CC-BY 3.0](http://creativecommons.org/licenses/by/3.0/)