(ns notify-me.office
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-me.models.validation.office :as office-rules]
   [notify-me.models.validation.user :as user-rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-me.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))


(defn get-office-id
  []
  (d/text (d/by-id "office-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent policy"
  []
  (let [id (get-office-id)]
    (= (count id) 0)))

(defn- prepare-office
  [office]
  (if (is-new?)
    office
    (assoc office :id (get-office-id))))

(defn is-office-unique?
  [office-name office-id]
  (if (and office-name (> (count office-name) 0))
    {:deferred (.ajax js/$ (format "%s/unique" office-name))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- validate-admin
  [office]
  (if (d/by-id "username")
    (let [admin (merge office {:roles [:office-admin]})]
      (binding [*is-unique?* (fn [x] true)] ;;new office user always is unique
       (validate admin user-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "office-form" (prepare-office admin))
                  (f/show-errors errors))))))
    (f/submit-form "office-form")))


(defn- validate-and-save
  [office]
  (f/remove-errors)
  (binding [*is-unique?* is-office-unique?]
    (validate office office-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (validate-admin office)
                  (f/show-errors errors))))))

(defn save-office
  [event]
  (let [office-values (f/serialize-form "office-form")]
    (validate-and-save office-values)))

(defn cancel
  [event])

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-office)
  (events/listen! (d/by-id "cancel")
                  :click
                  f/back))
