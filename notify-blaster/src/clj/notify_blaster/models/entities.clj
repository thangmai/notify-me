(ns notify-blaster.models.entities
  (:refer-clojure :exclude [comment])
  (:require (cemerick.friend [credentials :as creds]))
  (:use korma.core
        notify-blaster.utils))

(declare notification-recipient)

(defentity office)

(defentity user
  (belongs-to office)
  (prepare
   (fn [attributes]
     (-> (if (:password attributes)
           (assoc attributes :password (creds/hash-bcrypt (:password attributes)))
           attributes)
         (assoc :roles (str (:roles attributes)))
         (dissoc :password-match))))
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
  (belongs-to office))

(defentity notification
  (belongs-to office)
  (belongs-to delivery_policy)
  (has-many notification-recipient)
  (prepare #(str->int % :office_id :delivery_policy_id)))

(defentity notification_recipient
  (belongs-to notification))

(defentity message_delivery
  (belongs-to notification))

(defentity user_session
  (prepare #(merge % {:data (str (:data %))}))
  (transform #(deserialize % :data)))
