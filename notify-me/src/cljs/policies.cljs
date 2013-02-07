(ns notify-me.policies
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-me.models.validation.policy :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-me.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))

(defn- is-name-unique?
  "Deferred validator for the name uniqueness"
  [name policy-id]
  (if (and name (> (count name) 0))
    {:deferred (.ajax js/$ (format "%s/unique" name))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- prepare-policy
  [policy]
  (if (is-new?)
    policy
    (assoc policy :id (get-policy-id))))

(defn- validate-and-save
  "Validates the policy is a valid one and submits the form in that case"
  [policy]
  (f/remove-errors)
  (binding [*is-unique?* is-name-unique?]
    (validate policy rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "policy-form" (prepare-policy policy))
                  (f/show-errors errors))))))

(defn save-policy
  [event]
  (validate-and-save (f/serialize-form "policy-form")))

(defn get-policy-id
  []
  (d/text (d/by-id "policy-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent policy"
  []
  (let [id (get-policy-id)]
    (= (count id) 0)))

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-policy)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel))
