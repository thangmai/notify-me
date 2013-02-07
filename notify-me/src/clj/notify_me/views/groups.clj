(ns notify-me.views.groups
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn- create-contact-table
  [id contacts]
  [:table {:id id}
   [:thead [:tr [:th "Id"] [:th "Nombre"] [:th "Telefono"] [:th "Tipo"]]]
   [:tbody
    (map (fn [contact]
          [:tr
           [:td (:id contact)]
           [:td (:name contact)]
           [:td (:cell_phone contact)]
           [:td (:type contact)]])
        contacts)]])

(defn group-form
  [action group available-contacts errors]
  [:div {:id "group-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/groups/" (:id group))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name group)))
         (form/field
          (f/label "description" "Descripcion")
          (form/text-field action "description" (:description group)))
         (form/field
          (f/label "type" "Sincronizar")
          (f/check-box "type" (:type group) "Sincronizar con Ancel"))
         
         )
   [:div {:id "contacts"}
    [:div {:class "contact-table"}
     [:h4 "Contactos Disponibles"]
     (create-contact-table "available-contacts" available-contacts)]
    [:div {:class "contact-table"}
     [:h4 "Contactos Asignados"]
     (create-contact-table "assigned-contacts" (:members group))]
    ]
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])


(defn display-groups [groups]
  [:div {:id "shouts sixteen columns alpha omega"}
   (map
    (fn [group] [:h2 {:class "shout"} (h (:name group))])
    groups)])

(defn index [groups]
  (layout/common "Grupos"
                 [:div {:class "clear"}]
                 (display-groups groups)))
(defn- get-title
  [action group]
  (get {:new "Nuevo Grupo"
        :edit (:name group)} action))

(defn render-form
  ([action group users]
     (render-form action group users nil))
  ([action group available-users errors]
     (layout/common (get-title action group)
                    (group-form action group available-users errors)
                    [:div {:id "group-id" :style "display:none"} (:id group)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.group.main();"])))
