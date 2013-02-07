(ns notify-me.jobs.notifier
  (:require [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t])
  (:use [clojurewerkz.quartzite.jobs :only [defjob]]
        [clojurewerkz.quartzite.schedule.simple :only [schedule repeat-forever with-interval-in-seconds]]))

(defjob Notifier
  [ctx]
  (println "This is not a comment..."))

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
