(ns notify-me.controllers.users
  (:refer-clojure :exclude [read-string])
  (:require
   [notify-me.models.user :as model]
   [notify-me.views.users :as view]
   [ring.util.response :as res]
   [clojure.edn :refer [read-string]]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.user :as user-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils]))


(defn all
  "Renders a view with all the defined users"
  []
  (view/index (model/search {:office_id (current-office-id)})))

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

(defn configuration
  [id]
  (when (= id (str (current-user-id)))
    (when-let [user (model/one {:id (current-user-id)})]
      (view/render-form :configuration user))))

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
  (comment (println params)))

(defn create!
  "Creates a new user, all users are created as standard users, no admins"
  [params]
  (let [user (merge params {:office_id (current-office-id)})]
    (validate-and-save user (fn [] (model/create! user)))))

(defn update!
  "Updates an existing user, if passwords fields are posted password
  is changed, otherwise the fields are left as-is in the database"
  [params]
  (let [id (:id params)
        password (or (:password params) "xxxxxx")
        password-match (or (:password-match params) "xxxxxx")
        user (merge params {:password password :password-match password-match})]
    (validate-and-save
     user (fn []
            (let [u (if (:password params) user params)]
              (model/update! {:id (str->int id)} (dissoc u :id)))))))

(defn delete!
  [id]
  (when-let [user (model/one {:id (str->int id)})]
    (model/delete! user)
    (all)))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (GET "/:id/configuration" [id] (configuration id))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:username" [username] (show username))
  (GET "/:username/edit" [username] (edit username))
  (GET "/:username/delete" [username] (delete! username))
  (GET "/:name/unique" [name] (pr-str (is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (is-unique? name id)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))
