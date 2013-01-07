(ns notify-blaster.views.users
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-blaster.views.layout :as layout]
            [hiccup.form :as form]))

(defn show-new [])
(defn show [&args])
(defn edit [&args])

(defelem input-button
  [name text]
  [:input {:type "button" :value text :id name}])

(defn user-form []
  [:div {:id "shout-form" :class "sixteen columns alpha omega"}
   (form/form-to [:post "/offices/save"]
                 [:p
                  (form/label "name" "Nombre de la oficina")
                  (form/text-field "name")]
                 [:p
                  (form/label "description" "Descripcion")
                  (form/text-field "description")]
                 [:p
                  (form/submit-button "Guardar")
                  (input-button "cancel" "Cancelar")]
            )])

(defn display-users [offices]
  [:div {:id "shouts sixteen columns alpha omega"}
   (map
    (fn [office] [:h2 {:class "shout"} (h (:name office))])
    offices)])

(defn index [offices]
  (layout/common "User"
                 (user-form)
                 [:div {:class "clear"}]
                 (display-users offices)))