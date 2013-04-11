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
   [:thead [:tr [:th "Id"] [:th "Nombre"] [:th "Teléfono"] [:th "Tipo"]]]
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
          (f/label "description" "Descripción")
          (form/text-field action "description" (:description group))))
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


(defn display-groups
  [groups]
  (layout/create-entity-table "entity-table"
                              [[:name "Nombre"]
                               [:description "Descripción"]
                               [:type "Tipo"]]
                              groups
                              [["/groups/%s/edit" "Editar"]
                               ["/groups/%s/delete" "Borrar"]]))

(defn index
  [groups]
  (layout/common :groups
                 "Grupos"
                 (layout/button-new "Nuevo Grupo" "/groups/new")
                 (display-groups groups)))

(defn- get-title
  [action group]
  (get {:new "Nuevo Grupo"
        :edit (:name group)} action))

(defn render-form
  ([action group users]
     (render-form action group users nil))
  ([action group available-users errors]
     (layout/common :groups
                    (get-title action group)
                    (group-form action group available-users errors)
                    [:div {:id "group-id" :style "display:none"} (:id group)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.group.main();"])))
