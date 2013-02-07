(ns notify-me.views.session
  (:use [hiccup.core :only [html h]]
        [hiccup.page :only [doctype]]
        [hiccup.def :only [defelem]])
  (:require [notify-me.views.layout :as layout]
            [hiccup.form :as form]))


(defn login-form
  []
  [:div {:id "login-form" :class "sixteen columns alpha omega"}
   (form/form-to [:post "/login"]
                 [:p
                  (form/label "username" "User")
                  (form/text-field "username")]
                 [:p
                  (form/label "password" "Password")
                  (form/password-field "password")]
                 [:p
                  (form/submit-button "Log In")])])

(defn show-new
  [req]
  (layout/common "Login"
                 (login-form)))
