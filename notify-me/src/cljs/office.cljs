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

(def ^{:dynamic true} *fn-test* (fn [] "root"))

(defmulti test-method :type)
(defmethod test-method :something [x]
  (*fn-test*))

(defn bug []
  (binding [*fn-test* (fn [] "dynamic") ]
    (.log js/console (test-method {:type :something}))))


(defn bug2 []
  (binding [*fn-test* (fn [] "dynamic") ]
    (map #(test-method {:type :something}) (range 10))))

(defn is-unique2?
  [office-name]
  (when office-name
    (let [x (jq-macros/let-ajax [response {:url (format "%s/unique" office-name)}]
                                (reader/read-string response))]
      (do
        (.log js/console x) x))))

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
    (let [admin (merge office {:roles [:user]})]
      (binding [*is-unique?* (fn [x] true)] ;;new office user always is unique
       (validate admin user-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/submit-form "office-form")
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
  [event]
  (.log js/console (is-unique? "presidencia")))

(defn ^:export main
  []
  (events/listen! (d/by-id "save")
                  :click
                  save-office)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel))
