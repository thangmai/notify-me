(defproject notify-me "0.4.0-SNAPSHOT"
  :description "Notify-Me Web App"
  :url "http://notifyme.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "0.2.10"]
            [lein-midje "2.0.1"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [slingshot "0.10.3"]
                 ;;web server
                 [compojure "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 ;;database handler
                 [lobos "1.0.0-SNAPSHOT"]
                 [korma "0.3.0-RC4"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 ;;client side, html/javascript
                 [org.clojure/clojurescript "0.0-1552"]
                 [domina "1.0.2-SNAPSHOT"]
                 [hiccup "1.0.2"]
                 [jayq "0.3.2"]
                 ;;translation
                 [com.taoensso/tower "1.2.0"]
                 ;;authentication
                 [com.cemerick/friend "0.1.2"]
                 [crypto-random "1.1.0"]
                 ;;asterisk
                 [clj-asterisk "0.2.0"]
                 ;;sms library
                 [clj-ancel-sms "0.1.0"]
                 [robert/hooke "1.3.0"]
                 ;;quartz wrapper for jobs
                 [clojurewerkz/quartzite "1.0.1"]
                 ;;charts
                 [incanter "1.5.0-SNAPSHOT"]
                 ;;testing
                 [midje "1.4.0"]
                 ;;logging
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [clj-logging-config "1.9.10"]]
  :source-paths ["src/clj"]
  :hooks [leiningen.cljsbuild]
  :main notify-me.server
  :profiles {:pallet {:source-paths ["pallet/src"] :resource-paths []
                      :dependencies [[com.palletops/pallet "0.8.0-beta.7"]
                                     [com.palletops/java-crate "0.8.0-beta.2"]
                                     [com.palletops/git-crate "0.8.0-alpha.1"]
                                     [com.palletops/postgres-crate "0.8.0-314-SNAPSHOT"]
                                     [com.palletops/runit-crate "0.8.0-alpha.1"]]}}
  :cljsbuild {
              :crossovers [notify-me.models.validation]
              :crossover-path "crossover-cljs"
              :crossover-jar false
              :builds [{:source-path "src/cljs"
                        :compiler {
                                   :output-to "resources/public/scripts/notify-me.js"
                                   :optimization :whitespace
                                   :pretty-print true}}]})

