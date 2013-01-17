(ns notify-blaster.views.contacts
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-blaster.views.layout :as layout]
            [hiccup.form :as f]
            [notify-blaster.views.forms :as form]))

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
          (form/text-field action "cell_phone" (:cell_phone contact)))
         
         )

   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])


(defn display-contacts [contacts]
  [:div {:id "shouts sixteen columns alpha omega"}
   (map
    (fn [contact] [:h2 {:class "shout"} (h (:name contact))])
    contacts)])

(defn index [contacts]
  (layout/common "Contactos"
                 [:div {:class "clear"}]
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
                    [:script {:type "text/javascript" :language "javascript"} "notify_blaster.contact.main();"])))
