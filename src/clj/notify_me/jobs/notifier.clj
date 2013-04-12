(ns notify-me.jobs.notifier
  (:require [notify-me.jobs.dispatcher :as dispatcher]
            [notify-me.models.notification :as notifications]
            [dispatchers.call :as call]
            [dispatchers.sms :as sms]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t]
            [clojure.tools.logging :as log])
  (:use [clojurewerkz.quartzite.jobs :only [defjob]]
        [clojurewerkz.quartzite.schedule.simple :only [schedule repeat-forever with-interval-in-seconds]]))

(defn start-notification
  [notification]
  (log/info "Dispatching notification " (:id notification) " of type " (:type notification))
  (dispatcher/dispatch notification))

(defn pending-notifications
  []
  (let [pending (notifications/search (or (= :status "CREATED")
                                          (= :status "CANCELLING")))]
    (doall (map start-notification pending))))

(defjob Notifier
  [ctx]
  (log/info "Notifier job running")
  (pending-notifications))

(defn job
  []
  (j/build
   (j/of-type Notifier)
   (j/with-identity (j/key "jobs.notifier.1"))))

(defn id
  []
  (t/key "triggers.notifier.1"))

(defn trigger
  []
  (t/build
   (t/with-identity (id))
   (t/start-now)
   (t/with-schedule (schedule
                     (repeat-forever)
                     (with-interval-in-seconds 30)))))
