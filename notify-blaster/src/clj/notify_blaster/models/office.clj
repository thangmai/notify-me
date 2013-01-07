(ns notify-blaster.models.office
  (:require notify-blaster.models.db
            [notify-blaster.models.entities :as e])
  (:use korma.core
        notify-blaster.utils)

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