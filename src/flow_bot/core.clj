(ns flow-bot.core
  (:require
    [clojure.core.async :as async]
    [clojure.java.shell :as sh]
    [clojure.tools.logging :as log]
    [compojure.core :refer [defroutes GET PUT POST DELETE routes]]
    [environ.core :refer [env]]
    [flow-bot.event :as event]
    [mount.core :as mount]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.json :refer [wrap-json-body]]
    [ring.util.response :as response])
  (:gen-class))

(def event-queue (async/chan 10))

(mount/defstate event-processor
  :start (do (prn "Starting event-processor")
             (async/go-loop []
               (let [msg (async/<! event-queue)]
                 (event/handle-event! msg)
                 (recur)))))


(defroutes handler
  (GET "/" _req (response/response "This service is API-only. Please refer to documentation"))
  (POST "/" {:keys [headers body]}
    (let [event (assoc body :event-type (get headers "x-github-event"))
          repo-name (get-in event [:repository :name])]
      (log/info "Received event from " repo-name ": " (:event-type event))
      (log/debug event)
      (async/>!! event-queue event))
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