(ns flow-bot.event
  (:require
    [clojure.java.shell :as sh]
    [clojure.string :as str]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]
    [tentacles.issues :as issues]
    [tentacles.data :as git]
    [tentacles.orgs :as orgs]
    [tentacles.pulls :as pulls]
    [tentacles.repos :as repos]
    [clojure.string :as string]))

(defonce app-state (atom {:authors {}}))

(defn create-server-pr [branch-name pr-title original-pr-id]
  (pulls/create-pull (env :server-org)
                     (env :server-repo)
                     pr-title
                     "master"
                     branch-name
                     {:auth (env :auth)
                      :body (format "This is the code that appeared on %s/%s#%s"
                                    (env :client-org)
                                    (env :client-repo)
                                    original-pr-id)}))

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


(defn client-prs []
  (pulls/pulls (env :client-org) (env :client-repo)))

(defn pr-author [org repo pr-id]
  (let [commits (pulls/commits org repo pr-id {:auth (env :auth)})
        commit (first commits)]
    {:name  (get-in commit [:commit :author :name])
     :email (get-in commit [:commit :author :email])}))
;;;

(defn add-author! [sha author]
  (swap! app-state #(assoc-in % [:authors sha] author)))

(defn remove-author! [sha]
  (swap! app-state #(update % :authors dissoc sha)))

;;;

(defmulti handle-event!
  (fn [req] (:event-type req)))

(defmethod handle-event! :default [event]
  (log/debug "UNHANDLED EVENT " (:event-type event)))

(defmethod handle-event! "push" [event]
  (when (and (= (env :server-repo) (get-in event [:repository :name]))
             (= "refs/heads/master" (get-in event [:ref]))) ;; We're only interested in push events to master in server repo
    (log/info "Received a push on master branch on server repo, syncing client-repo")
    (let [head-commit-sha (get-in event [:head_commit :id])
          original-author (get-in @app-state [:authors head-commit-sha])
          _ (when original-author (remove-author! head-commit-sha))
          commit-author (get-in event [:head_commit :author])
          author-name (or (:name original-author) (:name commit-author))
          author-email (or (:email original-author) (:email commit-author))
          message (get-in event [:head_commit :message])
          sanitized-message (string/trim (string/replace message #"\(#\d+\)" ""))]
      (log/info (format "Merging commit '%s' <%s> from %s <%s>" sanitized-message head-commit-sha author-name author-email))
      (log/info (sh/sh "sh" "-c" (format "./sync-client.sh %s %s %s '%s' '%s' %s"
                                         (env :server-repo)
                                         (env :client-repo)
                                         (env :client-folder)
                                         sanitized-message
                                         author-name
                                         author-email))))))

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
            clone-url-with-auth (str/replace clone-url "https://github.com/" (str "https://" (env :auth) "@github.com/"))
            branch (get-in pr [:head :ref])
            new-branch-name (str "client-" pr-id)
            new-pr-title (:title pr)
            author (pr-author (env :client-org) (env :client-repo) pr-id)
            author-name (:name author)
            author-email (:email author)]
        (log/info (format "Syncing %s - branch %s - PR #%s - Author '%s' <%s> - Msg: %s" clone-url-with-auth branch pr-id author-name author-email new-pr-title))
        (log/info (sh/sh "sh" "-c" (format "./sync-server.sh %s %s %s '%s' %s '%s' %s %s %s" clone-url-with-auth branch pr-id author-name author-email new-pr-title (env :server-repo) (env :client-repo) (env :client-folder))))
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
        server-pr-id (get-in event [:pull_request :number])
        closed? (= "closed" (:action event))
        merged? (get-in event [:pull_request :merged])
        client-pr? (re-find #"^client\-[0-9]+$" pr-branch)]
    (when (and (= (env :server-repo) (get-in event [:repository :name]))
               closed?
               merged?
               client-pr?)
      (log/info "Server PR coming originally from Client Repo has been merged")
      (let [client-pr-id (str/replace pr-branch #"client-" "")
            author (pr-author (env :server-org) (env :server-repo) server-pr-id)
            merge-commit-sha (get-in event [:pull_request :merge_commit_sha])
            _ (log/info (format "Saving commit %s to attribute to '%s' <%s>" merge-commit-sha (:name author) (:email author)))
            _ (add-author! merge-commit-sha author)
            _ (log/info (format "Retrieving info about Client PR #%s" client-pr-id))
            pr (client-pr client-pr-id)]
        (when (= "open" (:state pr))
          (log/info "Notifying user that their PR was merged upstream")
          (create-closing-comment client-pr-id)
          (log/info "Attempting to close the PR")
          (close-client-pr client-pr-id))))))