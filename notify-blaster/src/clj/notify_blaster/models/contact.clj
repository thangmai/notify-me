(ns notify-blaster.models.contact
  (:require notify-blaster.models.db
            [notify-blaster.models.entities :as e])
  (:use korma.core
        notify-blaster.utils))


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

(defn all
  []
  (select e/contact))