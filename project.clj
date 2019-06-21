(defproject flow-bot "1.0.0-SNAPSHOT"
  :profiles {:uberjar {:main         flow_bot.core
                       :omit-source  true
                       :aot          :all
                       :source-paths ["src"]
                       :uberjar-name "flow-bot.jar"}}
  :dependencies [[cheshire "5.8.1"]
                 [clj-http "3.9.1"]
                 [clj-jgit "0.8.10"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.26"]
                 [ring "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [tentacles "0.5.1"]]
  :min-lein-version "2.8.3"
  :repl-options {:init-ns flow-bot.core})
