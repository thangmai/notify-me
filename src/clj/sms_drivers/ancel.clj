(ns sms-drivers.ancel
  (:require [clj-ancel-sms.messaging :as messaging]
            [clj-ancel.sms.administration :as admin]
            [sms-drivers.driver :as driver]
            [notify-me.models.contact :as contact]
            [notify-me.models.group :as group])
  (:use [robert.hooke :only [add-hook]]
        [slingshot.slingshot :only [try+ throw+]]))

;;Protocol implementation for message dispatch

(defrecord SMSEmpresa [^String service ^String tracking])

(extend-type SMSEmpresa
  driver/SMSDriver

  (group-dispatching? [this] true)
  
  (sms-to-number [this number message]
    (try+
     (if-not (messaging/to-cellphone (:service this) number message)
         (throw+ {:type ::send-failed}))
     (catch :type e
       {:error (:type e)})))
  
  (sms-to-group [this group message]
    (try+
     (if-not (messaging/to-group (:service this) group-name message)
       (throw+ {:type ::send-failed}))
     (catch :type e
       {:error (:type e)})))

;;TODO: this configuration could be coming out of somewhere
(def driver (SMSEmpresa. "1" "1"))

;;Hooks for groups and contacts synchronization

(defn- register-phone-number
  [save-fn attributes]
  (when-let [contact (save-fn attributes)]
    (when (not (admin/phone-registered? (:service driver) (:address attributes)))
      (admin/register-phone (:service driver) (:tracking driver) (:address attributes)))
    contact))

(defn- unregister-phone-number
  [delete-fn contact]
  (when-let [contact (delete-fn contact)]
    (admin/unregister-phone (:service driver) (:tracking driver) (:address contact))
    contact))

;;TODO:if the phone number changes all the groups the contact belongs
;;to needs to be updated too
(defn- update-phone-number
  ""
  [update-fn conditions attributes]
  (let [current-contact (contact/one conditions)]
    (when-let [new-contact (update-fn conditions attributes)]
      (let [old-number (:address current-contact)
            new-number (:address new-contact)]
        (when (not= old-number new-number)
          (try+
           (admin/unregister-phone (:service driver) (:tracking driver) old-number)
           (admin/register-phone (:service driver) (:tracking driver) new-number)
           (catch :type e
             (log/error e)))))
      new-contact)))

(add-hook #'contact/create! #'register-phone-number)
(add-hook #'contact/delete! #'unregister-phone-number)
(add-hook #'contact/update! #'update-phone-number)

(defn- register-group
  [save-fn attributes]
  (when-let [group (save-fn attributes)]
    (when (not (admin/group-exists? (:service driver) (:name group)))
      (try+
       (admin/create-group (:service driver) (:name group))
       (doseq [contact (:members group)]
         (admin/add-phone-to-group (:service driver)
                                   (:address contact)
                                   (:name group)))
       (catch :type e
         (log/error e))))
    group))

(defn- unregister-group
  [delete-fn group]
  (when-let [group (delete-fn group)]
    (try+
     (admin/delete-group (:service driver) (:name group))
     (catch :type e
       (log/error e)))
    group)))

(defn- update-group
  [update-fn group]
  )

(add-hook #'group/create! #'register-group)
(add-hook #'group/delete! #'unregister-group)
(add-hook #'group/update! #'update-group)