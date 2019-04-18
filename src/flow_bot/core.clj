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
    [ring.util.response :as response])
  (:gen-class))

(defroutes handler
  (POST "/" {:keys [headers body]}
    (let [event (assoc body :event-type (get headers "x-github-event"))]
      (log/info "Received event: " (:event-type event))
      (log/debug event)
      (event/handle-event! event))
    (response/response "ok")))

(defn print-env []
  (log/info "Environment:" (select-keys env [:server-org :server-repo :server-git :client-org :client-repo :client-git :client-folder :auth :magic-string])))

(def app
  (-> (routes handler)
      (wrap-json-body {:keywords? true})))

(defn init-repos! []
  (log/info (sh/sh "sh" "-c" (format "./init-repos.sh %s %s %s %s" (env :server-repo) (env :server-git) (env :client-repo) (env :client-git)))))

(mount/defstate repos
  :start (do (log/info "Initializing repos")
             (init-repos!)))

(mount/defstate server
  :start (do (log/info "Starting server")
             (jetty/run-jetty #'app {:join? false
                                     :port  (Integer/parseInt (or (env :port) "3000"))}))
  :stop (do (log/info "Stopping server")
            (.stop server)))

(defn go []
  (print-env)
  (mount/start))

(defn -main []
  (go))