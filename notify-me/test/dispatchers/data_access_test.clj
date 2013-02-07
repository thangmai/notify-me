(ns dispatchers.data-access-test
  (:require [dispatchers.call :as d]
            [notify-me.models.notification :as notification]
            [notify-me.models.contact :as contact]
            [notify-me.models.group :as group]
            [notify-me.models.trunk :as trunk]
            [notify-me.models.policy :as policy]
            [notify-me.models.entities :as e])
  (:use korma.core))

(def data-atom (atom nil))

(defn create-data
  []
  (let [trunk1 (trunk/create! {:name "SIP Trunk International"
                               :capacity 1
                               :technology "SIP"
                               :number "1000"
                               :context "test-context"
                               :extension "1000"
                               :priority "1"
                               :callerid "99970"})
        policy1 (policy/create! {:name "test"})
        contact1 (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
        contact2 (contact/create! {:name "contact 2" :type "P" :cell_phone "2"})
        contact3 (contact/create! {:name "contact 3" :type "P" :cell_phone "3"})
        contact4 (contact/create! {:name "contact 4" :type "P" :cell_phone "4"})
        group1 (group/create! {:name "grupo 1" :members [contact1 contact2]})
        group2 (group/create! {:name "grupo 2" :members [contact3 contact4]})
        group3 (group/create! {:name "grupo 3" :members [contact1 contact2 contact3 contact4]})]
    {:contacts [contact1 contact2 contact3 contact4]
     :groups [group1 group2 group3]
     :trunks [trunk1]
     :policies [policy1]}))

(defn drop-data
  []
  (doall (map #(group/delete! %) (:groups @data-atom)))
  (doall (map #(contact/delete! %) (:contacts @data-atom)))
  (doall (map #(trunk/delete! %) (:trunks @data-atom)))
  (doall (map #(policy/delete! %) (:policies @data-atom))))

(defn get-contact
  [idx]
  (get-in @data-atom [:contacts (dec idx)]))

(defn get-group
  [idx]
  (get-in @data-atom [:groups (dec idx)]))

(defn get-trunk
  [idx]
  (get-in @data-atom [:trunks (dec idx)]))

(defn get-policy
  [idx]
  (get-in @data-atom [:policies (dec idx)]))

(def notif (atom nil))

(defn set-notification
  [n]
  (reset! notif n)
  @notif)

(defn find-recipient
  [recipient type notification]
  (first (select e/notification_recipient
                 (where {:recipient_id (:id recipient)
                         :recipient_type type
                         :notification (:id notification)}))))

(defn find-delivery
  [contact notification]
  (first (select e/message_delivery
                 (where {:recipient_id (:id contact)
                         :recipient_type "C"
                         :notification (:id notification)}))))

(defn find-notification
  [notification]
  (first (select e/notification
                 (where {:id (:id notification)}))))
