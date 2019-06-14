(ns flow-bot.util
  (:require
    [clojure.java.shell :as sh]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]))

(defn init-repos! []
  (log/info (sh/sh "sh" "-c" (format "./init-repos.sh %s %s %s %s %s %s"
                                     (env :server-repo)
                                     (env :server-git)
                                     (env :client-repo)
                                     (env :client-git)
                                     (env :git-user)
                                     (env :git-email)))))
