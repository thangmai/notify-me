(ns notify-me.views.notifications
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype include-css include-js]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))


(defmacro recipient-row
  [recipient type]
  `[:tr
   [:td (:id ~recipient)]
   [:td (:name ~recipient)]
   [:td ~type]])

(defn- create-contact-table
  [id contacts groups]
  [:table {:id id}
   [:thead [:tr [:th "Id"] [:th "Nombre"] [:th "Tipo"]]]
   [:tbody
    (concat
     (map #(recipient-row % "C") contacts)
     (map #(recipient-row % "G") groups))]])

(defn notification-form
  [notification policies trunks contacts groups]
  [:div {:id "notification-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form "/notifications/"
         (form/field
          (f/label "message" "Mensaje")
          (f/text-area "message"))
         [:tr [:td] [:td [:div {:class "ui360"} [:a {:href "#" :id "play-tts"} "Escuchar"]]]]
         (form/field
          (f/label "type" "Tipo de contacto")
          (f/drop-down "type" [["Llamada" "CALL"] ["SMS" "SMS"]] (:type notification)))
         (form/field
          (f/label "trunk_id" "Troncal")
          (f/drop-down "trunk_id"
                       (vec (map (fn [p] [(:name p) (:id p)]) trunks))
                       (:trunk_id notification)))
         (form/field
          (f/label "delivery_policy_id" "Reglas de Despacho")
          (f/drop-down "delivery_policy_id"
                       (vec (map (fn [p] [(:name p) (:id p)]) policies))
                       (:delivery_policy_id notification))))
   [:div {:id "contacts"}
    [:div {:class "contact-table"}
     [:h4 "Contactos Disponibles"]
     (create-contact-table "available-recipients" contacts groups)]
    [:div {:class "contact-table"}
     [:h4 "Contactos a Notificar"]
     (create-contact-table "assigned-recipients" [] [])]
    ]
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])


(defn display-notifications
  [notifications]
  (layout/create-entity-table "entity-table"
                              [[:status "Estado"]
                               [:created "Iniciada"]
                               [:type "Tipo"]
                               [:message "Mensaje"]]
                              notifications
                              []))

(defn index
  [notifications]
  (layout/common "Notificaciones"
                 [:input {:type "button" :value "Nueva Notificacion" :onclick "window.location='/notifications/new';"}]
                 (display-notifications notifications)))


(defn- get-title
  [notification]
  (if notification
    (:name notification)
    "Nueva Notificacion"))

(defn render-new
  [policies trunks contacts groups]
  (layout/common (get-title nil)
                 (notification-form nil policies trunks contacts groups)
                 [:div {:id "notification-id" :style "display:none"} ""]
                 (include-css "/css/360player.css")
                 [:script {:type "text/javascript" :language "javascript"} "notify_me.notification.main();"]
                 (include-js "/scripts/soundmanager2-nodebug-jsmin.js")
                 (include-js "/scripts/berniecode-animator.js")
                 (include-js "/scripts/360player.js")))

(defn render-edit
  [notification policies])
