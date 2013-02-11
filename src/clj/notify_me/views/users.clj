(ns notify-me.views.users
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn user-form
  [action user errors]
  [:div {:id "user-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/users/" (:id user))
         (form/field
          (f/label "username" "Usuario")
          (form/text-field action "username" (:username user)))
         (form/field
          (f/label "display_name" "Nombre completo")
          (form/text-field action "display_name" (:display_name user)))
         (form/field
          (f/label "email" "Email")
          (f/email-field "email" (:email user)))
         (form/field
          (f/label "password" "Clave")
          (f/password-field  "password" (and user "xxxxxxxx")))
         (form/field
          (f/label "password-match" "Reingrese la Clave")
          (f/password-field  "password-match"))
         )
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])


(defn display-users
  [users]
  (layout/create-entity-table "entity-table"
                              [[:username "Usuario"]
                               [:display_name "Nombre"]
                               [:email "Email"]]
                              users
                              [["/users/%s/edit" "Editar"]
                               ["/users/%s/delete" "Borrar"]]))

(defn index [users]
  (layout/common "Usuarios"
                 [:input {:type "button" :value "Nuevo Usuario" :onclick "window.location='/users/new';"}]
                 (display-users users)))

(defn- get-title
  [action user]
  (get {:new "Nuevo Usuario"
        :edit (:username user)} action))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action user]
     (render-form action user nil))
  ([action user errors]
     (layout/common (get-title action user)
                    (user-form action user errors)
                    [:div {:id "user-id" :style "display:none"} (:id user)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.user.main();"])))
