(ns notify-blaster.models.notification
  (:require notify-blaster.models.db
            [notify-blaster.models.entities :as e])
  (:use korma.core
        korma.db
        notify-blaster.utils))

(defn create-recipient
  "TODO: expand group recipient, add address to table?"
  [nid rcpt]
  (println rcpt)
  (let [recipient {:recipient_id (str->int (:id rcpt))
                   :recipient_type (:type rcpt)}]
    (insert e/notification_recipient (values (assoc recipient :notification nid)))))

(defn create!
  [attributes]
  (let [recipients (:members attributes)
        notification (dissoc attributes :members)]
    (transaction
     (let [n (insert e/notification (values notification))]
       (doall (map #(create-recipient (:id notification) %) recipients))))))

(defn update!
  [conditions attributes]
  (update e/notification
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/notification
                 (where conditions)
                 (limit 1))))

(defn search
  [conditions]
  (select e/notification (where conditions)))

(defn all
  []
  (select e/notification))