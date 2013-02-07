(ns notify-me.server
  (:require
   [notify-me.webserver :as webserver]
   [clojurewerkz.quartzite.scheduler :as qs]
   [notify-me.jobs.notifier :as notifier]))

(defn -main
  []
  (alter-var-root #'*read-eval* (constantly false))
  (qs/initialize)
  (qs/start)
  (qs/unschedule-job (notifier/id))
  (qs/schedule (notifier/job) (notifier/trigger))
  (webserver/start []))
