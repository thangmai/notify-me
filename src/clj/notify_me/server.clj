(ns notify-me.server
  (:gen-class)
  (:require
   [notify-me.webserver :as webserver]
   [sms-drivers.driver :as sms-driver]
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

(defrecord SmsDriver []
  Lifecycle
  (start [this]
    (sms-driver/load))
  (stop [this]
    (sms-driver/unload)))

(defrecord App [job-manager web-service sms-dispatcher]
  Lifecycle
  (start [_]
    (log/info "Starting Application")
    (start job-manager)
    (start web-service)
    (start sms-dispatcher))
  (stop [_]
    (log/info "Shutting Down Application")
    (stop job-manager)
    (stop web-service)
    (stop sms-dispatcher)))

(defn prod-system []
  (let [web-service (->JettyWeb)
        job-manager (->QuartzJobs)
        sms-dispatcher (->SmsDriver)]
    (->App job-manager web-service sms-dispatcher)))

(defn -main
  [& args]
  (when-let [s (prod-system)]
    (start s)
    (.. (Runtime/getRuntime) (addShutdownHook (proxy [Thread] []
                                                (run []
                                                  (stop s)))))))
