(ns dispatchers.sms
  (:require [notify-me.jobs.dispatcher :as dispatcher]
            [dispatchers.model :as model]
            [notify-me.models.group :as group-model]
            [sms-drivers.driver :as sms-driver]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]
        [sms-drivers.protocol]))

(def ^:dynamic *driver* nil)

(defmethod model/normal-clearing? "SMS"
  [notification result]
  (nil? (:error result)))

(defmethod model/get-cause-name "SMS"
  [notification result]
  (pr-str (or (:error result) :OK)))

(defn save-result
  [notification recipient result]
  (let [policies (model/get-policies notification)
        has-chance? (model/remaining-attempts? recipient notification policies)
        status (if (not (model/normal-clearing? notification result))
                 (or (and has-chance? "FAILED") "CANCELLED")
                 "CONNECTED")
        cause (model/get-cause-name notification result)]
    (log/info (format "Saving result %s for contact %s on notification %s" status (pr-str recipient) (:id notification)))
    (model/save-delivery recipient notification status cause)
    ;;so far update-group-results is not called since there is no group expansion
    (model/update-contact-result notification recipient (:type recipient) status)
    {:status status :recipient recipient}))

(defn- send-direct-sms
  [notification contact]
  (log/info "Sending message to number " (:address contact) " from notification " (:id notification))
  (let [number (:address contact)
        message (:message notification)
        result (sms-to-number *driver* number message)]
    (save-result notification (merge contact {:type "C"}) result)))

(defn- send-group-sms
  [notification group-rcpt]
  (log/info "Sending message to group " (:recipient_id group-rcpt) " from notification " (:id notification))
  (let [group-id (:recipient_id group-rcpt)
        group-name (:name (group-model/one {:id group-id}))
        message (:message notification)
        result (sms-to-group *driver* group-name message)]
    (save-result notification {:address group-name :id group-id :type "G"} result)))

(defn process
  "Main notification loop"
  [notification]
  (log/info "Processing SMS notification " (:id notification))
  (let [recipients (model/retrieve-rcpt notification)
        contact-rcpts (model/direct-contacts notification)
        group-rcpts (filter #(= "G" (:recipient_type %)) recipients)]
    (loop [contacts contact-rcpts groups group-rcpts]
      (cond
       (model/cancelling? notification) 
           (model/update-status! notification "STOPPED")
       (or (seq contacts)
           (seq groups)) (let [failed-contacts (filter #(= (:status %) "FAILED")
                                                       (map #(send-direct-sms notification %) contacts))
                               failed-groups (filter #(= (:status %) "FAILED")
                                                     (map #(send-group-sms notification %) groups))]
                           (recur (map :recipient failed-contacts)
                                  (map :recipient failed-groups)))))))

(defmethod dispatcher/dispatch "SMS"
  [notification]
  (when (not (model/cancelling? notification))
    (model/update-status! notification "RUNNING")
    (binding [*driver* (sms-driver/load)]
      (process notification)))
  (model/update-status! notification "FINISHED"))

