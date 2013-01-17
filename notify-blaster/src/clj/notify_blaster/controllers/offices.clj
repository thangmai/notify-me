(ns notify-blaster.controllers.offices
  (:use
   [compojure.core :only [defroutes GET POST]]
   [notify-blaster.utils]
   [notify-blaster.models.validation.core :only [validate *is-unique?*]])
  (:require
   [clojure.string :as str]
   [ring.util.response :as ring]
   [notify-blaster.models.office :as office-model]
   [notify-blaster.models.user :as user-model]
   [notify-blaster.views.offices :as view]
   [notify-blaster.models.validation.office :as office-rules]
   [notify-blaster.models.validation.user :as user-rules]))


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
     (is-office-unique? nil office-name))
  ([office-id office-name]
     (let [office (office-model/one {:name office-name})]
       (or (nil? office)
           (= (str (:id office)) office-id)))))

(defn- validate-and-save-user
  [office user]
  (if user
    (let [user (merge user {:office_id (:id office)})]
        (binding [*is-unique?* (fn [v] true)]
          (validate user user-rules/rules {}
                (fn [errors]
                  (if (empty? errors)
                    (user-model/create! user)
                    errors)))))
    office))

(defn- validate-and-save
  [office user]
  (binding [*is-unique?* is-office-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate office office-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (let [office-id (office-model/create! office)]
                    (validate-and-save-user office-id user))
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
  [id params]
  (office-model/update! {:id (str->int id)}
                 params))