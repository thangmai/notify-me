(ns notify-me.views.policies
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as f]
            [notify-me.views.forms :as form]))

(defn policy-form
  [action policy errors]
  [:div {:id "policy-form"}
   [:p  {:id "form-message" :style "display:none"}]
   (form/form (str "/policies/" (:id policy))
         (form/field
          (f/label "name" "Nombre")
          (form/text-field action "name" (:name policy)))
         (form/field
          (f/label "no_answer_retries" "Reintentos si no atiende")
          (form/text-field action "no_answer_retries" (:no_answer_retries policy)))
         (form/field
          (f/label "retries_on_error" "Reintentos en caso de error")
          (form/text-field action "retries_on_error" (:retries_on_error policy)))
         (form/field
          (f/label "retries_on_busy" "Reintentos por ocupado")
          (form/text-field action "retries_on_busy" (:retries_on_busy policy)))
         (form/field
          (f/label "busy_interval_secs" "Intervalo en ocupado(segs)")
          (form/text-field action "busy_interval_secs" (:busy_interval_secs policy)))
         (form/field
          (f/label "no_answer_interval_secs" "Intervalo si no atiende(segs)")
          (form/text-field action "no_answer_interval_secs" (:no_answer_interval_secs policy)))
         (form/field
          (f/label "no_answer_timeout" "Timeout no atiende(segs)")
          (form/text-field action "no_answer_timeout" (:no_answer_timeout policy))))
   (form/input-button "save" "Guardar")
   (form/input-button "cancel" "Cancelar")])

(defn display-policies
  [policies]
  (layout/create-entity-table "entity-table"
                              [[:name "Nombre"]
                               [:retries_on_error "Reint. Error"]
                               [:retries_on_busy "Reint. Ocupado"]
                               [:busy_interval_secs "Intervalo Ocupado"]
                               [:no_answer_retries "Reint. No Atiende"]
                               [:no_answer_interval_secs "Intervalo No Atiende"]]
                              policies
                              [["/policies/%s/edit" "Editar"]
                               ["/policies/%s/delete" "Borrar"]]))

(defn index
  [policies]
  (layout/common :policies
                 "Políticas de Despacho"
                 (layout/button-new "Nueva Política" "/policies/new")
                 (display-policies policies)))


(defn- get-title
  [action policy]
  (get {:new "Nuevas Políticas de Despacho"
        :edit (:name policy)} action))

(defn render-form
  ([action]
     (render-form action nil nil))
  ([action policy]
     (render-form action policy nil))
  ([action policy errors]
     (layout/common :policies
                    (get-title action policy)
                    (policy-form action policy errors)
                    [:div {:id "policy-id" :style "display:none"} (:id policy)]
                    [:script {:type "text/javascript" :language "javascript"} "notify_me.policies.main();"])))
