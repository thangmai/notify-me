(ns notify-blaster.views.layout
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [doctype include-css include-js]]))

(defn common [title & body]
  (html
   (doctype :html5)
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
    [:title title]
    (include-css "/css/formalize.css")
    (include-js "/scripts/jquery-1.7.2.min.js")
    (include-js "/scripts/jquery.formalize.min.js")]
   [:body
    [:div {:id "header"}
     [:h1 {:class "container"} "SHOUTER"]]
    [:script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;"]
    (include-js "/scripts/notify-blaster.js")
    [:div {:id "content" :class "container"} body]]))

(defn four-oh-four []
  (common "Page Not Found"
          [:div {:id "four-oh-four"}
           "The page you requested could not be found"]))