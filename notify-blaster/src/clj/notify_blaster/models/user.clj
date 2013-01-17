(ns notify-blaster.models.user
  (:require notify-blaster.models.db
            [notify-blaster.models.entities :as e])
  (:use korma.core
        notify-blaster.utils)

  (:import org.mindrot.jbcrypt.BCrypt))

(defn create!
  [attributes]
  ;; not sure why insert returns serialized roles
  (deserialize
   (insert e/user (values attributes))
   :roles))

(defn update!
  [conditions attributes]
  (update e/user
          (set-fields attributes)
          (where conditions)))

(defn one
  [conditions]
  (first (select e/user
                 (where conditions)
                 (limit 1))))

(defn all
  []
  (select e/user))

(defn search
  [conditions]
  (select e/user (where conditions)))

(defn for-user-page
  [conditions]
  (first (select e/user
                 (where conditions))))