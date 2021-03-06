(ns notify-me.views.notifications
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype include-css include-js]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]
            [notify-me.config :as config]))


(def recipient-icons
  {"C" "/images/man-figure.png"
   "G" "/images/elevator.png"})

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
         [:tr [:td (f/label "message" "Mensaje")] 
              [:td (f/text-area "message") 
              [:label {:id "message-counter" :sms-limit (:sms-limit config/opts)}]] ]
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

(defn- get-title
  [notification]
  (if notification
    (:name notification)
    "Nueva Notificación")) 


(def notification-icons
  {"CALL" "/images/phone.png"
   "SMS" "/images/chat.png"})

(defn- get-type-image
  [notification]
  [:img {:src (get notification-icons (:type notification))}])

(defn display-notifications
  [notifications]
  (layout/create-entity-table "entity-table"
                              [[:status "Estado"]
                               [:created "Iniciada"]
                               [:type "Tipo" get-type-image]
                               [:message "Mensaje"]]
                              notifications
                              [["/notifications/%s/view" "Ver"]
                               ["/notifications/%s/cancel" "Cancelar"]]
                              [1 :desc]))

(defn index
  [notifications]
  (layout/common :notifications
                 "Notificaciones"
                 (layout/button-new "Nueva Notificación" "/notifications/new")
                 (display-notifications notifications)))

(defn- recipient-table
  [recipients]
  [:table {:id "recipients"}
   [:thead [:tr [:th "Nombre"] [:th "Estado"] [:th "Intentos"] [:th "Fallas"]]]
   [:tbody
    (map (fn [r]
          [:tr
           [:td (:name r)]
           [:td (:last_status r)]
           [:td (:attempts r)]
           [:td (:failed r)]])
         recipients)]])

(defn- attempts-table
  [attempts]
  [:table {:id "attempts"}
   [:thead [:tr [:th "Nombre"] [:th "Estado"] [:th "Fecha"] [:th "Dirección"] [:th "Causa"]]]
   [:tbody
    (map (fn [a]
           [:tr
           [:td (:name a)]
           [:td (:status a)]
           [:td (:delivery_date a)]
           [:td (:delivery_address a)]
           [:td (:cause a)]])
         attempts)]])

(defn render-dashboard
  [notification attempts]
  (layout/common :notifications
                 (:message notification)
                 (include-css "/css/dashboard.css")
                 (get-title notification)
                 [:div {:id "charts"}
                  [:div {:id "recipients_chart"}
                   [:h4 "Resumen de recipientes"]
                   [:img {:src (format "/notifications/%s/rcpt-chart" (:id notification))}]]
                  [:div {:id "attempts_chart"}
                   [:h4 "Resumen de intentos totales"]
                   [:img {:src (format "/notifications/%s/attempts-chart" (:id notification))}]]]
                 [:div {:id "details"}
                  [:div {:id "recipients_detail"}
                   [:h4 "Detalle de recipientes"]
                   (recipient-table (:members notification))]
                  [:div {:id "attempts_detail"}
                   [:h4 "Detalle de intentos totales"]
                   (attempts-table attempts)]]
                 [:script {:type "text/javascript" :language "javascript"}
                  "$('#attempts').dataTable();$('#recipients').dataTable();"]))

(defn render-new
  [policies trunks contacts groups]
  (layout/common :notifications
                 (get-title nil)
                 (notification-form nil policies trunks contacts groups)
                 [:div {:id "notification-id" :style "display:none"} ""]
                 (include-css "/css/360player.css")
                 [:script {:type "text/javascript" :language "javascript"} "notify_me.notification.main();"]
                 (include-js "/scripts/soundmanager2-nodebug-jsmin.js")
                 (include-js "/scripts/berniecode-animator.js")
                 (include-js "/scripts/360player.js")))

(defn render-edit
  [notification policies])
