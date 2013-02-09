(ns notify-me.trunk
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-me.models.validation.trunk :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-me.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))

(defn- is-name-unique?
  "Deferred validator for the name uniqueness"
  [name trunk-id]
  (if (and name (> (count name) 0))
    {:deferred (.ajax js/$ (format "%s/unique" name))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- prepare-trunk
  [trunk]
  (if (is-new?)
    trunk
    (assoc trunk :id (get-trunk-id))))

(defn- validate-and-save
  "Validates the trunk is a valid one and submits the form in that case"
  [trunk]
  (f/remove-errors)
  (binding [*is-unique?* is-name-unique?]
    (validate trunk rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "trunk-form" (prepare-trunk trunk))
                  (f/show-errors errors))))))

(defn save-trunk
  [event]
  (validate-and-save (f/serialize-form "trunk-form")))

(defn get-trunk-id
  []
  (d/text (d/by-id "trunk-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent trunk"
  []
  (let [id (get-trunk-id)]
    (= (count id) 0)))

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-trunk)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel))
