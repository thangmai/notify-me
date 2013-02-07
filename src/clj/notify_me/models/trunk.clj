(ns notify-me.models.trunk
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils))


(defn create!
  [attributes]
  (insert e/trunk (values attributes)))

(defn update!
  [conditions attributes]
  (update e/trunk
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/trunk
                 (where conditions)
                 (limit 1))))

(defn search
  [conditions]
  (select e/trunk (where conditions)))

(defn delete!
  [trunk]
  (delete e/trunk
          (where {:id (:id trunk)})))

(defn all
  []
  (select e/trunk))
