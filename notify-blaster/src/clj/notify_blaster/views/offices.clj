(ns notify-blaster.views.offices
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-blaster.views.layout :as layout]
            [hiccup.form :as form]))

(defelem input-button
  [name text]
  [:input {:type "button" :value text :id name}])

(defmacro text-field
  [action name value]
  `(let [field# (form/text-field ~name ~value)
         disabled# (= ~action :show)]
     (if disabled#
       (assoc-in field# [1 :disabled] true)
       field#)))

(defn office-form
  [action office errors]
  [:div {:id "office-form" :class "sixteen columns alpha omega"}
   (form/form-to [:post (str "/offices/" (:id office))]
                 [:p
                  (form/label "name" "Nombre de la oficina")
                  (text-field action "name" (:name office))]
                 [:p
                  (form/label "description" "Descripcion")
                  (text-field action "description" (:description office))]
                 [:p
                  (input-button "save" "Guardar")
                  (input-button "cancel" "Cancelar")]
            )])

(defn display-offices [offices]
  [:div {:id "shouts sixteen columns alpha omega"}
   (map
    (fn [office] [:h2 {:class "shout"} (h (:name office))])
    offices)])

(defn index [offices]
  (layout/common "Office"
                 (office-form)
                 [:div {:class "clear"}]
                 (display-offices offices)))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action office]
     (render-form action office nil))
  ([action office errors]
     (layout/common "Office Form"
                    (office-form action office errors)
                    [:script {:type "text/javascript" :language "javascript"} "notify_blaster.office.main();"])))
