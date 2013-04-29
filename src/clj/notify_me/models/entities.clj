(ns notify-me.models.entities
  (:refer-clojure :exclude [comment])
  (:require (cemerick.friend [credentials :as creds]))
  (:use korma.core
        notify-me.utils))

(declare notification-recipient)

(defentity office)

(defentity user
  (belongs-to office)
  (prepare
   (fn [attributes]
     (let [attrs (-> (if (:password attributes)
                       (assoc attributes :password (creds/hash-bcrypt (:password attributes)))
                       attributes)
                     (dissoc :password-match))]
       (if (:roles attributes)
         (assoc attrs :roles (str (:roles attributes)))
         attrs))))
  (transform #(deserialize % :roles)))

(defentity contact
  (belongs-to office))

(defentity contact_group
  (belongs-to office))

(defentity contact_group_member
  (belongs-to contact_group)
  (belongs-to contact)
  (belongs-to office))

(defentity delivery_policy
  (belongs-to office)
  (prepare #(str->int % :retries_on_error :busy_interval_secs :no_answer_retries 
                      :no_answer_interval_secs :retries_on_busy :no_answer_timeout)))

(defentity trunk
  (belongs-to office)
  (prepare #(str->int % :capacity)))

(defentity sms_provider)

(defentity notification
  (belongs-to office)
  (belongs-to delivery_policy)
  (belongs-to trunk)
  (belongs-to sms_provider)
  (has-many notification-recipient)
  (prepare #(str->int % :office_id :delivery_policy_id :trunk_id)))

(defentity notification_recipient
  (belongs-to notification))

(defentity message_delivery
  (belongs-to notification))

(defentity user_session
  (prepare #(merge % {:data (str (:data %))}))
  (transform #(deserialize % :data)))
