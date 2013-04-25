(ns notify-me.views.session
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype include-css include-js]]
        [hiccup.def :only [defelem]])
  (:require [hiccup.form :as form]))

(defn layout [title & content]
  (html
   (doctype :html5)
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
    [:title title]
    (include-css "/css/login.css")
    (include-js "/scripts/jquery-1.7.2.min.js")
    (include-js "/scripts/jquery.formalize.min.js")
    ]
   [:body {:class "login"}
    [:div {:class "back"}
     [:div {:id "content" :class "container"}
      [:div {:class "logo"} "Notify Me"]
      content]]]))

(defn login-form
  []
  [:div {:id "login-form" :class "innerContainer"}
   (form/form-to [:post "/login"]
                 [:label {:for "username" :class "loginLabel"} "Email"]
                 [:input {:type  "text"
                          :name  "username"
                          :id    "username"
                          :value ""
                          :class "loginField"}]
                 [:label {:for "password" :class "loginLabel"} "Clave"]
                 [:input {:type  "password"
                          :name  "password"
                          :id    "password"
                          :value ""
                          :class "loginField"}]
                 [:input {:type "submit" :value "Log In" :class "loginButton"}])])

(defn show-new
  [req]
  (layout "Login"
          (login-form)))
