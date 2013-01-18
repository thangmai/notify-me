(ns notify-blaster.server
  (:require
   [notify-blaster.webserver :as webserver]
   [clojurewerkz.quartzite.scheduler :as qs]
   [notify-blaster.jobs.notifier :as notifier]))

(defn -main
  []
  (qs/initialize)
  (qs/start)
  (qs/schedule (notifier/job) (notifier/trigger))
  (webserver/start []))