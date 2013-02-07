(ns notify-me.views.forms
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [hiccup.form :as f]))


(defelem input-button
  [name text]
  [:input {:type "button" :value text :id name}])

(defmacro text-field
  [action name value]
  `(let [field# (f/text-field ~name ~value)
         disabled# (= ~action :show)]
     (if disabled#
       (assoc-in field# [1 :disabled] true)
       field#)))

(defmacro form
  [url & body]
  `(f/form-to [:post ~url]
                 [:table {:class "form_demo"}
                  [:tbody ~@body]]
                 )
  )

(defmacro field
  [label field]
  `[:tr [:th ~label] [:td ~field]])

