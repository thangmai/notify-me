(ns notify-blaster.views.notifications
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-blaster.views.layout :as layout]
            [hiccup.form :as f]
            [notify-blaster.views.forms :as form]))

(defn notification-form
  [action notification errors]
  [:div {:id "notification-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/policies/" (:id notification))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name notification)))
        
         (form/field
          (f/label "no_answer_retries" "Reintentos si no atiende")
          (form/text-field action "no_answer_retries" (:no_answer_retries notification)))

         (form/field
          (f/label "retries_on_error" "Reintentos en caso de error")
          (form/text-field action "retries_on_error" (:retries_on_error notification)))

         (form/field
          (f/label "busy_interval_secs" "Intervalo en ocupado(segs)")
          (form/text-field action "busy_interval_secs" (:busy_interval_secs notification)))

         (form/field
          (f/label "no_answer_interval_secs" "Intervalo si no atiende(segs)")
          (form/text-field action "no_answer_interval_secs" (:no_answer_interval_secs notification)))
         
         )

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
  [action notification]
  (get {:new "Nueva Notificacion"
        :edit (:name notification)} action))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action notification]
     (render-form action notification nil))
  ([action notification errors]
     (layout/common (get-title action notification)
                    (notification-form action notification errors)
                    [:div {:id "notification-id" :style "display:none"} (:id notification)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_blaster.notifications.main();"])))
