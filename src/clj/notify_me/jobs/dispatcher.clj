(ns notify-me.jobs.dispatcher)

;; Notification dispatch function entry point, receives the notification to process
(defmulti dispatch :type)

(defmethod dispatch :default
  [notification]
  (println "Dispatching unknown type " (:type notification)))