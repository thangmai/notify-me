(ns notify-blaster.office
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify_blaster.models.validation.core :only [validate *is-unique?*]]
   [notify_blaster.models.validation.office :only [rules]]
   [jayq.util :only [clj->js]])
  (:require
   [jayq.core :as jq]
   [domina :as d]
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

(defn is-unique?
  [office-name]
  (when office-name
    {:deferred (.ajax js/$ (format "%s/unique" office-name))
     :fn reader/read-string}
    ))

(defn- valid-office?
  [office]
  (binding [*is-unique?* is-unique?]
    (validate office rules {} (fn [errors] (js/alert errors)))))

(defn serialize-form
  [form-id]
  (let [a (.serializeArray (js/$ (str "#" form-id " > form")))]
    (reduce #(merge %1 {(get %2 "name") (get %2 "value")}) {} (js->clj a))))

(defn save-office
  [event]
  (let [office-values (serialize-form "office-form")]
    (valid-office? office-values)))

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