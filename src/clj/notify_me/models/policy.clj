(ns notify-me.models.policy
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils))

(defn create!
  [attributes]
  (insert e/delivery_policy (values attributes)))

(defn update!
  [conditions attributes]
  (update e/delivery_policy
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/delivery_policy
                 (where conditions)
                 (limit 1))))

(defn delete!
  [policy]
  (delete e/delivery_policy
          (where {:id (:id policy)})))

(defn search
  [conditions]
  (select e/delivery_policy (where conditions)))

(defn all
  []
  (select e/delivery_policy))
