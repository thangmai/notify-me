(ns notify-me.forms
  (:use
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [jayq.core :as jq]
   [domina :as d]
   [cljs.reader :as reader]))

(defn- submit-form
  [form-id]
  (.submit (js/$ (str "#" form-id " > form"))))

(defn create-post-data
  "Creates a url-encoded post data from a hashmap"
  [entity]
  (clojure.string/join (map #(str (name (% 0)) "=" (% 1) "&") entity)))

(defn display-message
  [text msg-type]
  (let [message-node (d/by-id "form-message")]
    (do
      (d/set-html! message-node text)
      (d/add-class! message-node msg-type)
      (d/set-styles! message-node {:display "block"}))))

(defn post-form
  "Issues an async post using AJAX to the url
  specified in the form 'form-id', it posts the entity
  and not the fields from the form"
  [form-id entity]
  (let [url (d/attr (sel (str "#" form-id " > form")) "action")
        post-data (pr-str entity)
        promise (.ajax js/$ url (clj->js {:url url :type "POST" :data post-data :contentType "text"}))]
    (.then promise
           (fn [response, status, jqxhr]
               (display-message "Guardado con exito" "notice-msg"))
           (fn [e]
               (display-message "Ooops, there was an error, please try again later" "error-msg")))))

(defn serialize-form
  [form-id]
  (let [a (.serializeArray (js/$ (str "#" form-id " > form")))]
    (reduce #(merge %1 {(keyword (get %2 "name")) (get %2 "value")}) {} (js->clj a))))

(defn- display-error
  [error]
  (let [field-name (error 0)
        err-msgs (error 1)]
    (do
      (d/add-class! (d/by-id field-name) "error")
      (d/add-class! (sel (format "label[for='%s']" (name field-name))) "error")
      ;;only show the first error message for each field
      (first err-msgs))))

(defn- remove-errors
  "Remove all form errors"
  []
  (d/remove-class! (d/by-class "error") "error")
  (d/set-styles! (d/by-id "form-message") {:display "none"})
  (d/set-classes! (d/by-id "form-message") ""))

(defn- show-errors
  "Marks each failed field as an error and displays a top-level error message
   with each error description"
  [errors]
  (let [messages (map display-error errors)
        error-list (str "<ul>" (clojure.string/join (map (fn [m] (str "<li>" m "</li>")) messages)) "</ul>")]
    (do
      (display-message error-list "error-msg"))))
