(ns notify-blaster.models.notification
  (:require notify-blaster.models.db
            [notify-blaster.models.entities :as e])
  (:use korma.core
        notify-blaster.utils))

(defn create!
  [attributes]
  (insert e/notification (values attributes)))

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