(ns dispatchers.sms
  (:require [notify-me.jobs.dispatcher :as dispatcher]
            [dispatchers.model :as model]
            [notify-me.sms-drivers.driver :as sms-driver])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def ^:dynamic *driver* nil)

(defmethod model/normal-clearing? "SMS"
  [notification result]
  (nil? (:error result)))

(defmethod model/get-cause-name "SMS"
  [notification result]
  (pr-str (:error result)))

(defn- save-result
  [notification recipient result]
  (let [policies (model/get-policies notification)
        has-chance? (model/remaining-attempts? recipient notification policies)
        status (if (model/normal-clearing? notification result) 
                 (or (and has_chance? "FAILED") "CANCELLED")
                 "CONNECTED")
        cause (model/get-cause-name notification result)]
    (model/save-delivery recipient notification status cause)
    (model/update-contact-result notification recipient (:type recipient) status)
    {:status status :recipient recipient}))

(defn- send-direct-sms
  [notification contact]
  (let [number (:address contact)
        message (:message notification)
        result (*driver*/sms-to-number number message)]
    (save-result notification (merge contact {:type "C"}) result))

(defn- send-group-sms
  [notification group-rcpt]
  (let [group-name (:recipient_id group-rcpt)
        message (:message notification)
        result (*driver*/sms-to-group group-name message)]
    (save-result notification {:address group-name :id group-name :type "G"} result))

(defn- process
  "Main notification loop"
  [notification]
  (log/debug "Processing notification")
  (let [recipients (model/retrieve-rcpt notification)
        contact-rcpts (model/direct-contacts notification)
        group-rcpts (filter #(= "G" (:recipient_type %)) recipients)]
    (loop [contacts contact-rcpts groups group-rcpts]
      (cond
       (model/cancelling? notification) (model/update-status! notification "STOPPED")
       (or (seq contacts)
           (seq groups)) (let [failed-contacts (filter #(= (:status %) "FAILED")
                                                       (map #(send-direct-sms notification %) contact-rcpts))
                               failed-groups (filter #(= (:status %) "FAILED")
                                                     (map #(send-group-sms notification %) group-rcpts))]
                           (recur (map :recipient failed-contacts)
                                  (map :recipient failed-groups)))))))

(defmethod dispatcher/dispatch "SMS"
  [notification]
  (model/update-status! notification "RUNNING")
  (binding [*driver* sms-driver/driver]
    (process notification))
  (model/update-status! notification "FINISHED"))

