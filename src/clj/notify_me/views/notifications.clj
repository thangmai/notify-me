(ns notify-me.views.notifications
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
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
     (map #(recipient-row % "G") groups))
    ]])

(defn notification-form
  [notification policies trunks contacts groups]
  [:div {:id "notification-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form "/notifications/"
              
         (form/field
          (f/label "message" "Mensaje")
          (f/text-area "message"))

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


(defn display-notifications [notifications]
  [:div {:id "shouts sixteen columns alpha omega"}
   (map
    (fn [n] [:h2 {:class "shout"} (h (:name n))])
    notifications)])

(defn index [notifications]
  (layout/common "Notificaciones"
                 [:div {:class "clear"}]
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
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.notification.main();"]))

(defn render-edit
  [notification policies])
