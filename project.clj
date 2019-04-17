(defproject flow-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cheshire "5.8.1"]
                 [clj-http "3.9.1"]
                 [clj-jgit "0.8.10"]
                 [compojure "1.6.1"]
                 [mount "0.1.14"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.26"]
                 [ring "1.6.1"]
                 [ring/ring-json "0.4.0"]
                 [tentacles "0.5.1"]]
  :repl-options {:init-ns flow-bot.core})
