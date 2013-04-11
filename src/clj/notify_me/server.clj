(ns notify-me.server
  (:gen-class)
  (:require
   [notify-me.webserver :as webserver]
   [clojurewerkz.quartzite.scheduler :as qs]
   [notify-me.jobs.notifier :as notifier]
   [clj-logging-config.log4j :as log-config]
   [clojure.tools.logging :as log]))

(defn -main
  [& args]
  (log/info "Starting")
  (qs/initialize)
  (qs/start)
  (qs/unschedule-job (notifier/id))
  (qs/schedule (notifier/job) (notifier/trigger))
  (webserver/start [])
  (qs/shutdown))
