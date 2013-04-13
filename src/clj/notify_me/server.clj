(ns notify-me.server
  (:gen-class)
  (:require
   [notify-me.webserver :as webserver]
   [clojurewerkz.quartzite.scheduler :as qs]
   [notify-me.jobs.notifier :as notifier]
   [clj-logging-config.log4j :as log-config]
   [clojure.tools.logging :as log]))

(defprotocol Lifecycle
  (start [this] "Start all life")
  (stop [this] "End all life"))

(defrecord QuartzJobs []
  Lifecycle
  (start [_]
    (qs/initialize)
    (qs/start)
    (qs/schedule (notifier/job) (notifier/trigger)))
  (stop [_]
    (qs/unschedule-job (notifier/id))
    (qs/shutdown)))

(defrecord JettyWeb []
  Lifecycle
  (start [this]
    (webserver/start))
  (stop [this]
    (webserver/stop)))

(defrecord App [job-manager web-service]
  Lifecycle
  (start [_]
    (log/info "Starting Application")
    (start job-manager)
    (start web-service))
  (stop [_]
    (log/info "Shutting Down Application")
    (stop job-manager)
    (stop web-service)))

(defn prod-system []
  (let [web-service (->JettyWeb)
        job-manager (->QuartzJobs)]
    (->App job-manager web-service)))

(defn -main
  [& args]
  (when-let [s (prod-system)]
    (start s)
    (.. (Runtime/getRuntime) (addShutdownHook (proxy [Thread] []
                                                (run []
                                                  (stop s)))))))
