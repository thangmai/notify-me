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
  [notification]
  (select e/notification_recipient
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
