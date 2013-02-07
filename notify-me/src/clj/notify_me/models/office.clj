
(ns notify-me.models.office
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils)

  (:import org.mindrot.jbcrypt.BCrypt))

(declare one)

(defn create!
  [attributes]
  (insert e/office (values attributes)))

(defn update!
  [conditions attributes]
  (update e/office
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/office
                 (where conditions)
                 (limit 1))))

(defn all
  []
  (select e/office))
