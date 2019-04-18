(ns flow-bot.core
  (:require
    [clojure.java.shell :as sh]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]]
    [compojure.core :refer [defroutes GET PUT POST DELETE routes]]
    [flow-bot.event :as event]
    [mount.core :as mount]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.json :refer [wrap-json-body]]
    [ring.util.response :as response]))

(defroutes handler
  (POST "/" {:keys [headers body]}
    (let [event (assoc body :event-type (get headers "x-github-event"))]
      (log/debug event)
      (event/handle-event! event))
    (response/response "ok")))

(def app
  (-> (routes handler)
      (wrap-json-body {:keywords? true})))

(mount/defstate server
  :start (do (log/info "Server started")
             (jetty/run-jetty #'app {:join? false
                                     :port  3000}))
  :stop (do (log/info "Stopping server")
            (.stop server)))


(defn init-repos! []
  (log/info (sh/sh "sh" "-c" (format "./init-repos.sh %s %s %s %s" (env :server-repo) (env :server-git) (env :client-repo) (env :client-git)))))

(defn go []
  (mount/start))