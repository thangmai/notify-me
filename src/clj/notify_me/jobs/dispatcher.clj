(ns notify-me.jobs.dispatcher
  (:require [clojure.tools.logging :as log]))

;; Notification dispatch function entry point, receives the notification to process
(defmulti dispatch :type)

(defmethod dispatch :default
  [notification]
  (log/error "Dispatching unknown type " (:type notification)))