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

(defn- get-title
  [notification]
  (if notification
    (:name notification)
    "Nueva Notificacion")) 

(defn display-notifications
  [notifications]
  (layout/create-entity-table "entity-table"
                              [[:status "Estado"]
                               [:created "Iniciada"]
                               [:type "Tipo"]
                               [:message "Mensaje"]]
                              notifications
                              [["/notifications/%s/view" "Ver"]
                               ["/notifications/%s/cancel" "Cancelar"]]))

(defn index
  [notifications]
  (layout/common "Notificaciones"
                 (layout/button-new "Nueva Notificacion" "/notifications/new")
                 (display-notifications notifications)))

(defn- recipient-table
  [recipients]
  [:table {:id "recipients"}
   [:thead [:tr [:th "Nombre"] [:th "Tipo"] [:th "Estado"] [:th "Intentos"] [:th "Fallas"] [:th "Conexiones"]]]
   [:tbody
    (map (fn [r]
          [:tr
           [:td (:name r)]
           [:td (:recipient_type r)]
           [:td (:last_status r)]
           [:td (:attempts r)]
           [:td (:failed r)]
           [:td (:connected r)]])
         recipients)]])

(defn- attempts-table
  [attempts]
  [:table {:id "attempts"}
   [:thead [:tr [:th "Nombre"] [:th "Tipo"] [:th "Estado"] [:th "Fecha"] [:th "Direccion"] [:th "Causa"]]]
   [:tbody
    (map (fn [a]
           [:tr
           [:td (:name a)]
           [:td (:recipient_type a)]
           [:td (:status a)]
           [:td (:delivery_date a)]
           [:td (:delivery_address a)]
           [:td (:cause a)]])
         attempts)]])

(defn render-dashboard
  [notification attempts]
  (layout/common (:message notification)
                 (include-css "/css/dashboard.css")
                 (get-title notification)
                 [:div {:id "charts"}
                  [:div {:id "recipients_chart"}
                   [:div "Summary recipientes"]
                   [:img {:src (format "/notifications/%s/rcpt-chart" (:id notification))}]]
                  [:div {:id "attempts_chart"}
                   [:div "Summar intentos totales"]
                   [:img {:src (format "/notifications/%s/attempts-chart" (:id notification))}]]]
                 [:div {:id "details"}
                  [:div {:id "recipients_detail"}
                   [:div "Recipientes"]
                   (recipient-table (:members notification))]
                  [:div {:id "attempts_detail"}
                   [:div "Intentos totales"]
                   (attempts-table attempts)]]
                 [:script {:type "text/javascript" :language "javascript"}
                  "$('#attempts').dataTable();$('#recipients').dataTable();"]))

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
