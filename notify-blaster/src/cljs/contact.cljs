(ns notify-blaster.contact
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-blaster.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-blaster.models.validation.contact :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-blaster.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))

(defn- is-phone-unique?
  "Deferred validator for the phone uniqueness"
  [phone contact-id]
  (if (and phone (> (count phone) 0))
    {:deferred (.ajax js/$ (format "%s/unique" phone))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- validate-and-save
  "Validates the contact is a valid one and submits the form in that case"
  [contact]
  (f/remove-errors)
  (binding [*is-unique?* is-phone-unique?]
    (validate contact rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "contact-form" contact)
                  (f/show-errors errors))))))

(defn save-contact
  [event]
  (validate-and-save (f/serialize-form "contact-form")))

(defn get-contact-id
  []
  (d/text (d/by-id "contact-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent contact"
  []
  (let [id (get-contact-id)]
    (= (count id) 0)))

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-contact)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel))