(ns notify-me.views.offices
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn when-new
  [office & body]
  (when-not office
     (map identity body)))

(defn office-form
  [action office errors]
  [:div {:id "office-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/offices/" (:id office))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name office)))
         (form/field
          (f/label "description" "Descripción")
          (form/text-field action "description" (:description office)))
         (when-new office
           [:tr [:th [:h3 "Administrador"]] [:td]]
           (form/field
            (f/label "username" "Nombre")
            (form/text-field action "username" ""))
           (form/field
            (f/label "email" "Email")
            (f/email-field "email"))
           (form/field
            (f/label "password" "Clave")
            (f/password-field "password"))
           (form/field
            (f/label "password-match" "Repita la Clave")
            (f/password-field "password-match"))))

   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])

(defn display-offices
  [offices]
  (layout/create-entity-table "entity-table"
                              [[:name "Nombre"] [:description "Descripcion"]]
                              offices
                              [["/offices/%s/edit" "Editar"]
                               ["/offices/%s/delete" "Borrar"]]))

(defn index [offices]
  (layout/common "Oficinas"
                 [:input {:type "button" :value "Nueva Oficina" :onclick "window.location='/offices/new';"}]                                                   
                 (display-offices offices)))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action office]
     (render-form action office nil))
  ([action office errors]
     (layout/common "Nueva Oficina"
                    (office-form action office errors)
                    [:div {:id "office-id" :style "display:none"} (:id office)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.office.main();"])))
