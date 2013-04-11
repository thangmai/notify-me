(ns notify-me.controllers.offices
  (:refer-clojure :exclude [read-string])
  (:use
   [compojure.core :only [defroutes GET POST]]
   [notify-me.utils]
   [notify-me.models.validation.core :only [validate *is-unique?*]])
  (:require
   [clojure.string :as str]
   [ring.util.response :as ring]
   [clojure.edn :refer [read-string]]
   [notify-me.models.office :as office-model]
   [notify-me.models.user :as user-model]
   [notify-me.models.policy :as policy-model]
   [notify-me.views.offices :as view]
   [notify-me.models.validation.office :as office-rules]
   [notify-me.models.validation.user :as user-rules]))


(defn all
  "Renders a view with all the defined offices"
  []
  (view/index (office-model/all)))

(defn show
  "Renders an office form read-only"
  [id]
  (when-let [office (office-model/one {:id (str->int id)})]
    (view/render-form :show office)))

(defn edit
  "Renders an edition form for the specified office"
  [id]
  (when-let [office (office-model/one {:id (str->int id)})]
    (view/render-form :edit office)))

(defn show-new
  "Render empty office form"
  []
  (view/render-form :new))

(defn is-office-unique?
  ([office-name]
     (is-office-unique? office-name nil))
  ([office-name office-id]
     (let [office (office-model/one {:name office-name})]
       (or (nil? office)
           (= (str (:id office)) office-id)))))

(defn- validate-and-save-user
  [office user]
  (if user
    (let [user (merge user {:office_id (:id office)})]
        (binding [*is-unique?* (fn [n v] true)]
          (validate user user-rules/rules {}
                (fn [errors]
                  (if (empty? errors)
                    (user-model/create! user)
                    errors)))))
    office))

(defn- create-default-policy
  "Creates default policy dispatch rules for the office
   All values take the default ones defined on the database schema"
  [office]
  (policy-model/create! {:name "default"
                         :office_id (:id office)}))

(defn- validate-and-save
  [office user]
  (binding [*is-unique?* is-office-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate office office-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (let [office (office-model/create! office)]
                    (validate-and-save-user office user)
                    (create-default-policy office))
                  errors)))))

(defn- append-user-defaults
  "Default user role on new user"
  [params]
  (merge
   (select-keys params [:username :password :password-match :email])
   {:roles [:user]}))

(defn create!
  "Creates a new office, assumes first user gets posted with the
  parameter data"
  [params]
  (let [office (select-keys params [:name :description])
        admin (append-user-defaults params)]
    (validate-and-save office admin)))

(defn update!
  "Updates an existing office"
  [params]
  (let [id (:id params)
        office (dissoc params :id)]
    (office-model/update! {:id (str->int id)}
                          office)))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:id" [id] (show id))
  (GET "/:id/edit" [id] (edit id))
  (GET "/:name/unique" [name] (pr-str (is-office-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (is-office-unique? id name)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))
