(ns flow-bot.event
  (:require
    [clojure.java.shell :as sh]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]
    [tentacles.issues :as issues]
    [tentacles.orgs :as orgs]
    [tentacles.pulls :as pulls]
    [tentacles.repos :as repos]))

(defn create-server-pr [branch-name pr-title original-pr]
  (pulls/create-pull (env :server-org)
                     (env :server-repo)
                     pr-title
                     "master"
                     branch-name
                     {:auth (env :auth)
                      :body (format "This is the code that appeared on %s/%s#%s" (env :server-org) (env :client-repo) original-pr)}))

(defn client-pr [id]
  (pulls/specific-pull (env :client-org) (env :client-repo) id {:auth (env :auth)}))


(defn close-client-pr [id]
  (pulls/edit-pull (env :client-org) (env :client-repo) id {:auth  (env :auth)
                                                     :title "New title"
                                                     :body  "this should be closed"
                                                     :state "closed"}))

(defn server-branch-exists? [branch-name]
  (contains? (set (map :name (repos/branches (env :server-org) (env :server-repo) {:auth (env :auth)})))
             branch-name))

(defn create-closing-comment [id]
  (issues/create-comment (env :client-org) (env :client-repo) id "Thanks for contributing! This code has been merged upstream!" {:auth (env :auth)}))

;;;

(defmulti handle-event!
  (fn [req] (:event-type req)))

(defmethod handle-event! :default [event]
  (log/debug "UNHANDLED EVENT " (:event-type event)))

(defmethod handle-event! "push" [event]
  "We're only interested in push events to master in server repo"
  (when (and (= (env :server-repo) (get-in event [:repository :name]))
             (= "refs/heads/master" (get-in event [:ref])))
    (log/info "Received a push on master branch on server repo, syncing client-repo")
    (log/info (sh/sh "sh" "-c" (format "./sync-client.sh %s %s %s" (env :server-repo) (env :client-repo) (env :client-folder))))))

(defmethod handle-event! "issue_comment" [event]
  (let [pr-id (get-in event [:issue :number])
        user (get-in event [:comment :user :login])
        org (get-in event [:organization :login])
        owner? (orgs/member? org user {:auth (env :auth)})]
    (when (and (= (env :client-repo) (get-in event [:repository :name]))
               owner?
               (get-in event [:comment :body])
               (str/includes? (get-in event [:comment :body]) (env :magic-string)))
      (log/info "Detected a PR comment saying it's okay to merge PR: " pr-id)
      (let [pr (client-pr pr-id)
            clone-url (get-in pr [:head :repo :clone_url])
            branch (get-in pr [:head :ref])
            new-branch-name (str "client-" pr-id)
            new-pr-title (:title pr)
            author "Daniel Janus"
            author-email "dj@danieljanus.pl"]
        (log/info (format "Syncing %s - branch %s - PR #%s - Author %s <%s> - Msg: %s" clone-url branch pr-id author author-email new-pr-title))
        (log/info (sh/sh "sh" "-c" (format "./sync-server.sh %s %s %s '%s' %s '%s'" clone-url branch pr-id author author-email new-pr-title)))
        (if (server-branch-exists? new-branch-name)
          (do
            (log/info "Branch successfully created, creating pull request!" new-branch-name new-pr-title)
            (let [result (create-server-pr new-branch-name new-pr-title pr-id)]
              (if (= "open" (:state result))
                (log/info "PR on server created succesfully")
                (log/error "Error when creating server PR"))))
          (log/error "ERROR WHEN SYNCING CLIENT TO SERVER"))))))

(defmethod handle-event! "pull_request" [event]
  (let [pr-branch (get-in event [:pull_request :head :ref])
        closed? (= "closed" (:action event))
        merged? (get-in event [:pull_request :merged])]
    (when (and (= (env :server-repo) (get-in event [:repository :name]))
               closed?
               merged?)
      (log/info "Server PR coming originally from Client Repo has been merged")
      (let [client-pr-id (str/replace pr-branch #"client-" "")
            _ (log/info (format "Retrieving info about Client PR #%s" client-pr-id))
            pr (client-pr client-pr-id)]
        (when (= "open" (:state pr))
          (log/info "Notifying user that their PR was merged upstream")
          (create-closing-comment client-pr-id)
          (log/info "Attempting to close the PR")
          (close-client-pr client-pr-id))))))