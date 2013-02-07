(ns notify-me.models.sms_provider
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils))


(defn create!
  [attributes]
  (insert e/sms_provider (values attributes)))

(defn update!
  [conditions attributes]
  (update e/sms_provider
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/sms_provider
                 (where conditions)
                 (limit 1))))

(defn search
  [conditions]
  (select e/sms_provider (where conditions)))

(defn delete!
  [sms_provider]
  (delete e/sms_provider
          (where {:id (:id sms_provider)})))

(defn all
  []
  (select e/sms_provider))
