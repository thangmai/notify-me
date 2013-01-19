(ns notify-blaster.views.notifications
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-blaster.views.layout :as layout]
            [hiccup.form :as f]
            [notify-blaster.views.forms :as form]))


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
  [notification policies contacts groups]
  [:div {:id "notification-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form "/notifications/"
              
         (form/field
          (f/label "message" "Mensaje")
          (f/text-area "message"))
         
         (form/field
          (f/label "sms" "Enviar SMS")
          (f/check-box "sms"))
         
         (form/field
          (f/label "call" "Llamar")
          (f/check-box "call"))

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
  [policies contacts groups]
  (layout/common (get-title nil)
                    (notification-form nil policies contacts groups)
                    [:div {:id "notification-id" :style "display:none"} ""]
                    [:script {:type "text/javascript" :language "javascript"} "notify_blaster.notification.main();"]))

(defn render-edit
  [notification policies])
