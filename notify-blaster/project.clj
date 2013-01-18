(defproject notify-blaster "0.1.0-SNAPSHOT"
  :description "Notify Blaster Web App"
  :url "http://notifyblaster.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "0.2.10"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 ;;web server
                 [compojure "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 ;;database handler
                 [lobos "1.0.0-SNAPSHOT"]
                 [korma "0.3.0-beta9"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.clojure/java.jdbc "0.1.1"]
                 ;;client side, html/javascript
                 [org.clojure/clojurescript "0.0-1572"]
                 [domina "1.0.0"]
                 [hiccup "1.0.2"]
                 [jayq "0.3.2"]
                 ;;translation
                 [com.taoensso/tower "1.2.0"]
                 ;;authentication
                 [com.cemerick/friend "0.1.2"]
                 [crypto-random "1.1.0"]
                 ;;quartz wrapper for jobs
                 [clojurewerkz/quartzite "1.0.1"]]
  :source-paths ["src/clj"]
  :hooks [leiningen.cljsbuild]
  :main notify-blaster.server
  :cljsbuild {
              :crossovers [notify-blaster.models.validation]
              :crossover-path "crossover-cljs"
              :crossover-jar false
              :builds [{:source-path "src/cljs"
                        :compiler {
                                   :output-to "resources/public/scripts/notify-blaster.js"
                                   :optimization :whitespace
                                   :pretty-print true}}]})

