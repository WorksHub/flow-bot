(ns flow-bot.util
  (:require
    [clojure.java.shell :as sh]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]))

(defn server-git []
  (str "https://" (env :git-user) ":" (env :git-token) "@github.com/" (env :server-org) "/" (env :server-repo)))

(defn client-git []
  (str "https://" (env :git-user) ":" (env :git-token) "@github.com/" (env :client-org) "/" (env :client-repo)))

(defn init-repos! []
  (log/info (sh/sh "sh" "-c" (format "./init-repos.sh %s %s %s %s %s %s"
                                     (env :server-repo)
                                     (server-git)
                                     (env :client-repo)
                                     (client-git)
                                     (env :git-user)
                                     (env :git-email)))))
