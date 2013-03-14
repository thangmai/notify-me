(ns notify-me.server
  (:require
   [notify-me.webserver :as webserver]
   [clojurewerkz.quartzite.scheduler :as qs]
   [notify-me.jobs.notifier :as notifier]
   [clj-logging-config.log4j :as log-config]))

(defn -main
  []
  (qs/initialize)
  (qs/start)
  (qs/unschedule-job (notifier/id))
  (qs/schedule (notifier/job) (notifier/trigger))
  (webserver/start [])
  (qs/shutdown))
