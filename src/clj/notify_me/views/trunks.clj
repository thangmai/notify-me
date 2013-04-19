(ns notify-me.views.trunks
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn trunk-form
  [action trunk errors]
  [:div {:id "trunk-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/trunks/" (:id trunk))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name trunk)))
         (form/field
          (f/label "technology" "Tecnología")
          (f/drop-down "technology" [["SIP" "SIP"]] (:technology trunk)))
         (form/field
          (f/label "number" "Peer de Salida")
          (form/text-field action "number" (:number trunk)))
         (form/field
          (f/label "context" "Contexto")
          (form/text-field action "context" (:context trunk)))
         (form/field
          (f/label "priority" "Prioridad")
          (form/text-field action "priority" (:priority trunk)))
         (form/field
          (f/label "extension" "Extensión")
          (form/text-field action "extension" (:extension trunk)))
         (form/field
          (f/label "callerid" "Caller ID")
          (form/text-field action "callerid" (:callerid trunk)))
         (form/field
          (f/label "prefix" "Prefijo")
          (form/text-field action "prefix" (:prefix trunk)))
         (form/field
          (f/label "capacity" "Líneas Disponibles")
          (form/text-field action "capacity" (:capacity trunk)))
         (form/field
          (f/label "host" "Host")
          (form/text-field action "host" (:host trunk)))
         (form/field
          (f/label "user" "Usuario")
          (form/text-field action "user" (:user trunk)))
         (form/field
          (f/label "password" "Password")
          (form/text-field action "password" (:password trunk))))
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])

(defn display-trunks
  [trunks]
  (layout/create-entity-table "entity-table"
                              [[:host "Host"]
                               [:name "Nombre"]
                               [:technology "Tecnología"]
                               [:number "Número"]
                               [:extension "Extensión"]
                               [:priority "Prioridad"]
                               [:callerid "Caller Id"]
                               [:capacity "Capacidad"]]
                              trunks
                              [["/trunks/%s/edit" "Editar"]
                               ["/trunks/%s/delete" "Borrar"]]))

(defn index
  [trunks]
  (layout/common :trunks
                 "Troncales"
                 (layout/button-new "Nuevo Troncal" "/trunks/new")
                 (display-trunks trunks)))

(defn- get-title
  [action trunk]
  (get {:new "Nuevo Troncal"
        :edit (:name trunk)} action))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action trunk]
     (render-form action trunk nil))
  ([action trunk errors]
     (layout/common :trunks
                    (get-title action trunk)
                    (trunk-form action trunk errors)
                    [:div {:id "trunk-id" :style "display:none"} (:id trunk)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.trunk.main();"])))
