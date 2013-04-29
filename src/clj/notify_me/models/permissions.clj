(ns notify-me.models.permissions
  (:require [cemerick.friend :as friend]))

(def moderator-usernames (clojure.string/split (get (System/getenv) "MODERATOR_NAMES" "admin") #","))

(defn moderator? [username]
  (some #(= % username) moderator-usernames))

(defn current-username []
  (:username (friend/current-authentication)))

(defn current-displayname []
  (:display_name (friend/current-authentication)))

(defn current-user-id []
  (:id (friend/current-authentication)))

(defn current-office-id []
  (:office_id (friend/current-authentication)))

(defn can-modify-profile? [user]
  (or
   (= user (current-username))
   (= (:username user) (current-username))))

(defn can-modify-record?
  ([record]
     (can-modify-record? record (friend/current-authentication)))
  ([record owner-map]
     (or
      (= (:username record) (:username owner-map))
      (= (:user_id record) (:id owner-map))
      (moderator? (:username owner-map)))))

;; Pretty sure there's something in onlisp about this
(defmacro protect [check & body]
  `(if (not ~check)
     (ring.util.response/redirect "/")
     (do ~@body)))

(def actions-by-role
  (array-map :admin #{:offices}
             :user #{:notifications :contacts :groups :configuration}
             :office-admin #{:users :trunks :policies :notifications :contacts :groups}))

(defn get-user-actions
  []
  (let [user (friend/current-authentication)
        roles (:roles user)]
    (reduce #(clojure.set/union %1 (get actions-by-role %2)) #{} roles)))