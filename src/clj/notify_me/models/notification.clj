(ns notify-me.models.notification
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        korma.db
        notify-me.utils))

(defn create-recipient
  "TODO: expand group recipient, add address to table?"
  [nid rcpt]
  (let [recipient {:recipient_id (str->int (:id rcpt))
                   :recipient_type (:type rcpt)}]
    (insert e/notification_recipient (values (assoc recipient :notification nid)))))

(defn create!
  [attributes]
  (let [recipients (:members attributes)
        notification (dissoc attributes :members)]
    (transaction
     (let [n (insert e/notification (values notification))]
       (doall (map #(create-recipient (:id notification) %) recipients))
       (assoc n :members recipients)))))

(defn update!
  [conditions attributes]
  (update e/notification
          (set-fields attributes)
          (where conditions)))


(defn- get-recipients
  "Retrieves the complete list of recipients for the given notification.
   Joins against contact and contact_group tables in order to retrieve the corresponding name
   for each recipient so this function is not suitable for correct pagination. FIX this"
  [notification]
  (concat
   (select e/notification_recipient
           (fields :recipient_id :contact_group.name :recipient_type :last_status :attempts :failed :connected)
           (join e/contact_group (= :contact_group.id :notification_recipient.recipient_id))
           (where {:notification (:id notification)
                   :recipient_type "G"}))
   (select e/notification_recipient
           (fields :recipient_id :contact.name :recipient_type :last_status :attempts :failed :connected)           
           (join e/contact (= :contact.id :notification_recipient.recipient_id))
           (where {:notification (:id notification)
                   :recipient_type "C"}))))

(defn attempts
  "Retrieves the complete list of attempts for a specific notification
   This function is not pagination friendly either FIX(this should be done first querying a page and
   joining after that)"
  [notification]
  (concat
   (select e/message_delivery
           (fields :recipient_id :contact_group.name :recipient_type :status :delivery_date :delivery_address :cause)
           (join e/contact_group (= :contact_group.id :message_delivery.recipient_id))
           (where {:notification (:id notification)
                   :recipient_type "G"}))
   (select e/message_delivery
           (fields :recipient_id :contact.name :recipient_type :status :delivery_date :delivery_address :cause)           
           (join e/contact (= :contact.id :message_delivery.recipient_id))
           (where {:notification (:id notification)
                   :recipient_type "C"}))))

(defn attempts-summary
  [notification]
   (select e/message_delivery
          (fields :status)
          (aggregate (count :*) :cnt :status)
          (group :status)
          (where {:notification (:id notification)})))

(defn recipients-summary
  [notification]
  (select e/notification_recipient
          (fields :last_status)
          (aggregate (count :*) :cnt :last_status)
          (group :last_status)
          (where {:notification (:id notification)})))

(defn one
  [conditions]
  (when-let [n (first (select e/notification
                              (where conditions)
                              (limit 1)))]
    (assoc n :members (get-recipients n))))

(defn search
  [conditions]
  (select e/notification (where conditions)))

(defn delete!
  [notification]
  (transaction
   (delete e/notification_recipient
           (where {:notification (:id notification)}))
   (delete e/message_delivery
           (where {:notification (:id notification)}))
   (delete e/notification
           (where {:id (:id notification)}))))

(defn all
  []
  (select e/notification))
