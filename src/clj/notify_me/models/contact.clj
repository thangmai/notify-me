(ns notify-me.models.contact
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils))


(defn create!
  [attributes]
  (insert e/contact (values attributes)))

(defn update!
  [conditions attributes]
  (update e/contact
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/contact
                 (where conditions)
                 (limit 1))))

(defn search
  [conditions]
  (select e/contact (where conditions)))

(defn delete!
  [contact]
  (delete e/contact
          (where {:id (:id contact)})))

(defn all
  []
  (select e/contact))
