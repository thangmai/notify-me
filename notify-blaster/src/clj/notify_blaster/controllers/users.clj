(ns notify-blaster.controllers.users
  (:require
   [notify-blaster.models.user :as model]
   [notify-blaster.views.users :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-blaster.models.validation.user :as user-rules])
  (:use
   [notify-blaster.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-blaster.models.validation.core :only [validate *is-unique?*]]
   [notify-blaster.utils]))


(defn all
  "Renders a view with all the defined users"
  []
  (view/index (model/all)))

(defn show
  "Renders a user form read-only"
  [id]
  (when-let [user (model/one {:id (str->int id)})]
    (view/render-form :show user)))

(defn is-unique?
  ([username]
     (is-unique? username nil))
  ([username user-id]
     (let [office-id (current-office-id)
           user (model/one {:username username :office_id office-id})]
       (or (nil? user)
           (= (str (:id user)) user-id)))))

(defn edit
  "Renders an edition form for the specified user"
  [id]
  (when-let [user (model/one {:id (str->int id)})]
    (view/render-form :edit user)))

(defn show-new
  "Render empty user form"
  []
  (view/render-form :new))

(defn- validate-and-save
  [user callback]
  (binding [*is-unique?* is-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate user user-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (callback)
                  errors)))))

(defn create-new!
  [params]
  (println params))

(defn create!
  "Creates a new user, all users are created as standard users, no admins"
  [params]
  (let [user (merge params {:roles [:user] :office_id (current-office-id)})]
    (validate-and-save user (fn [] (model/create! user)))))

(defn update!
  "Updates an existing user, if passwords fields are posted password
  is changed, otherwise the fields are left as-is in the database"
  [id params]
  (let [password (or (:password params) "xxxxxx")
        password-match (or (:password-match params) "xxxxxx")
        user (merge params {:password password :password-match password-match})]
    (validate-and-save
     user (fn [] (let [u (if (:password params) user params)]
                   (model/update! {:id (str->int id)} (dissoc u :id)))))))