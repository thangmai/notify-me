(ns notify-blaster.user
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-blaster.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-blaster.models.validation.user :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-blaster.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))

(def validate-password (atom nil))

(defn- is-user-unique?
  "Deferred validator for the username uniqueness"
  [username user-id]
  (if (and username (> (count username) 0))
    {:deferred (.ajax js/$ (format "%s/unique" username))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- get-rules
  "If the user isn't new and the password wasn't changed
   removes the password validation rules"
  []
  (if (and (not (is-new?)) (not @validate-password))
    (dissoc rules/rules :password :password-match)
    rules/rules))

(defn- validate-and-save
  "Validates the user is a valid one and submits the form in that case"
  [user]
  (f/remove-errors)
  (binding [*is-unique?* is-user-unique?]
    (validate user (get-rules) {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "user-form" user)
                  (f/show-errors errors))))))

(defn- get-user
  "Retrieves the current user values from the input fields.
   The function defaults the user role to :user in order to pass validaton since it's not optional
   If the user isn't new and the password wasn't changed, it does not submit that field"
  []
  (let [user-values (f/serialize-form "user-form")
        user (merge user-values {:roles [:user]})]
    (if (and (not (is-new?)) (not @validate-password))
      (dissoc user :password :password-match)
      user)))

(defn save-user
  [event]
  (validate-and-save (get-user)))

(defn get-user-id
  []
  (d/text (d/by-id "user-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent user"
  []
  (let [id (get-user-id)]
    (= (count id) 0)))

(defn on-password-changed
  [event]
  (reset! validate-password true))

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-user)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel)
  (events/listen! (d/by-id "password")
                  :keyup
                  on-password-changed)
  (events/listen! (d/by-id "password")
                  :change
                  on-password-changed))