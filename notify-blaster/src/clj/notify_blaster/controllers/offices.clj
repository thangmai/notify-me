(ns notify-blaster.controllers.offices
  (:use
   [compojure.core :only [defroutes GET POST]]
   [notify-blaster.utils]
   [notify-blaster.models.validation.core :only [validate]])
  (:require
   [clojure.string :as str]
   [ring.util.response :as ring]
   [notify-blaster.models.office :as model]
   [notify-blaster.views.offices :as view]
   [notify-blaster.models.validation.office :as validation]))


(defn all
  "Renders a view with all the defined offices"
  []
  (view/index (model/all)))

(defn show
  "Renders an office form read-only"
  [id]
  (when-let [office (model/one {:id (str->int id)})]
    (view/render-form :show office)))

(defn edit
  "Renders an edition form for the specified office"
  [id]
  (when-let [office (model/one {:id (str->int id)})]
    (view/render-form :edit office)))

(defn show-new
  "Render empty office form"
  []
  (view/render-form :new))

(defn is-unique?
  [office-name]
  (nil? (model/one {:name office-name})))

(defn- valid-office?
  [office]
  (binding [notify-blaster.models.validation.core/*is-unique?* is-unique?]
    (validate office validation/rules)))

(defn create!
  [params]
  (let [name (:name params)
        description (:description params)
        errors (valid-office? params)]
    (if (empty? errors)
      (do
        (model/create! params)
        (ring/redirect "/offices/new"))
      (view/render-form :edit params errors))))

(defn update!
  "Updates an existing office"
  [id params]
  (model/update! {:id (str->int id)}
                 params))