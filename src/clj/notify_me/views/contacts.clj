(ns notify-me.views.contacts
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn contact-form
  [action contact errors]
  [:div {:id "contact-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/contacts/" (:id contact))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name contact)))
         (form/field
          (f/label "type" "Tipo")
          (f/drop-down "type" [["Persona" "P"] ["Oficina" "O"]] (:type contact)))
         (form/field
          (f/label "cell_phone" "Telefono")
          (form/text-field action "cell_phone" (:cell_phone contact))))
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])

(defn display-contacts
  [contacts]
  (layout/create-entity-table "entity-table"
                              [[:name "Nombre"]
                               [:type "Tipo"]
                               [:cell_phone "Celular"]]
                              contacts
                              [["/contacts/%s/edit" "Editar"]
                               ["/contacts/%s/delete" "Borrar"]]))

(defn index [contacts]
  (layout/common "Contactos"
                 (layout/button-new "Nuevo Contacto" "/contacts/new")
                 (display-contacts contacts)))

(defn- get-title
  [action contact]
  (get {:new "Nuevo Contacto"
        :edit (:name contact)} action))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action contact]
     (render-form action contact nil))
  ([action contact errors]
     (layout/common (get-title action contact)
                    (contact-form action contact errors)
                    [:div {:id "contact-id" :style "display:none"} (:id contact)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.contact.main();"])))
