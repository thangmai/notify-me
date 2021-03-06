(ns notify-me.models.user
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        notify-me.utils)

  (:import org.mindrot.jbcrypt.BCrypt))

(defn create!
  [attributes]
  (insert e/user (values attributes)))

(defn update!
  [conditions attributes]
  (update e/user
          (set-fields attributes)
          (where conditions)))

(defn delete!
  [user]
  (delete e/user
          (where {:id (:id user)})))

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
