(ns flow-bot.core
  (:require
    [clojure.tools.logging :as log]
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
  :start (do (jetty/run-jetty #'app {:join? false
                                     :port  3000})
             (log/info "Server started"))
  :stop (do (log/info "Stopping server")
            (.stop server)))


(defn go []
  (mount/start))