(ns sms-drivers.ancel
  (:require [clj-ancel-sms.messaging :as messaging]
            [clj-ancel-sms.administration :as admin]
            [notify-me.models.contact :as contact]
            [notify-me.models.group :as group]
            [clojure.tools.logging :as log])
  (:use [robert.hooke :only [add-hook]]
        [sms-drivers.protocol]
        [slingshot.slingshot :only [try+ throw+]]))

;;Protocol implementation for message dispatch

(defrecord SMSEmpresa [^String service ^String tracking]
  SMSDriver
  (group-dispatching? [this] true)
  
  (sms-to-number [this number message]
    (try+
     (if-not (messaging/to-cellphone service number message)
       {:error {:type ::send-failed}})
     (catch :type e
       {:error (:type e)})
     (catch Object _
       {:error (pr-str (:throwable &throw-context))})))
  
  (sms-to-group [this group message]
    (try+
     (if-not (messaging/to-group service group message)
       {:error {:type ::send-failed}})
     (catch :type e
       {:error (:type e)})
     (catch Object _
       {:error (pr-str (:throwable &throw-context))}))))

;;TODO: this configuration could be coming out of somewhere
(def driver (SMSEmpresa. "1" "1"))

;;Hooks for groups and contacts synchronization

(defn- register-phone-number
  "Registers the phone number when a new contact is saved"
  [save-fn attributes]
  (when-let [contact (save-fn attributes)]
    (try+
     (when (not (admin/phone-registered? (:service driver) (:cell_phone attributes)))
       (admin/register-phone (:service driver) (:tracking driver) (:cell_phone attributes)))
     (catch :type e
       (log/error e))
     (catch Object _
       (log/error (:throwable &throw-context))))
    contact))

(defn- unregister-phone-number
  "Deletes current phone number registration if the number does
   not exists for a different contact in a different office"
  [delete-fn c]
  (when-let [deleted (delete-fn c)]
    (when (nil? (seq (contact/search {:cell_phone (:cell_phone c)})))
      (try+ 
       (admin/unregister-phone (:service driver) (:tracking driver) (:cell_phone c))
       (catch :type e
         (log/error e))
       (catch Object _
         (log/error (:throwable &throw-context)))))
    deleted))

(defn- update-phone-number
  "Updates phone registration when phone changed and belonging groups"
  [update-fn conditions attributes]
  (let [current-contact (contact/one conditions)]
    (when-let [new-contact (update-fn conditions attributes)]
      (let [old-number (:cell_phone current-contact)
            new-number (:cell_phone new-contact)]
        (when (not= old-number new-number)
          (try+
           (admin/unregister-phone (:service driver) (:tracking driver) old-number)
           (when (not (admin/phone-registered? (:service driver) new-number))
             (admin/register-phone (:service driver) (:tracking driver) new-number))
           ;;groups should be searched by contact since same number may have not changed
           ;;for another office
           (let [phone-groups (group/groups-for-contact (:id current-contact))]
             (doseq [group phone-groups]
               (admin/rmv-phone-from-group (:service driver)
                                           old-number
                                           (:name group))
               (admin/add-phone-to-group (:service driver)
                                         new-number
                                         (:name group))))
           (catch :type e
             (log/error e))
           (catch Object _
             (log/error (:throwable &throw-context))))))
      new-contact)))

(add-hook #'contact/create! #'register-phone-number)
(add-hook #'contact/delete! #'unregister-phone-number)
(add-hook #'contact/update! #'update-phone-number)

(defn- register-group
  "Creates a new group in the ANCEL database"
  [save-fn attributes]
  (when-let [group (save-fn attributes)]
    (try+
     (when (not (admin/group-exists? (:service driver) (:name group)))
       (admin/create-group (:service driver) (:name group))
       (doseq [contact (:members group)]
         (admin/add-phone-to-group (:service driver)
                                   (:cell_phone contact)
                                   (:name group))))
     (catch :type e
       (log/error e))
     (catch Object _
       (log/error (:throwable &throw-context))))
    group))

(defn- unregister-group
  "Deletes the group from ANCEL database"
  [delete-fn group]
  (when-let [group (delete-fn group)]
    (try+
     (admin/delete-group (:service driver) (:name group))
     (catch :type e
       (log/error e))
     (catch Object _
       (log/error (:throwable &throw-context))))
    group))

(defn- update-group
  "When a group changes, remove the phones not there anymore and add the new ones"
  [update-fn conditions attributes]
  (let [current-group (group/one conditions)]
    (when-let [new-group (update-fn conditions attributes)]
      (println (:members new-group))
      (let [old-members (:members current-group)
            new-members (:members new-group)
            to-delete (filter (fn [old] 
                                (not (seq (filter #(= (:id old) (:id %)) new-members))))
                              old-members)
            to-add (filter (fn [new] 
                             (not (seq (filter #(= (:id new) (:id %)) old-members))))
                           new-members)]
      (doseq [d to-delete]
        (try+
         (admin/rmv-phone-from-group (:service driver)
                                   (:cell_phone d)
                                   (:name current-group))
         (catch :type e
           (log/error e))
         (catch Object _
           (log/error (:throwable &throw-context)))))
      (doseq [a to-add]
        (try+ 
         (admin/add-phone-to-group (:service driver)
                                   (:cell_phone a)
                                   (:name current-group))
         (catch :type e
           (log/error e))
         (catch Object _
           (log/error (:throwable &throw-context))))))
      new-group)))

(add-hook #'group/create! #'register-group)
(add-hook #'group/delete! #'unregister-group)
(add-hook #'group/update! #'update-group)