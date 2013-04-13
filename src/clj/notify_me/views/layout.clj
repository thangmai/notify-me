(ns notify-me.views.layout
  (:require [notify-me.models.permissions :as permissions]
            [ring.util.response :as response])
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [doctype include-css include-js]]))

(defn- header
  [title]
  [:div
   [:div {:class "grid_4 offset_4"}
    [:h1 "Notify Me"]]
   [:div {:class "grid_4"}
    [:div {:class "logged-user"} (permissions/current-displayname)] [:a {:href "/logout"} "Logout"]]])

(defn- footer
  []
  [:div {:class "grid_12"} "2013 © Copyright Intception . All rights reserved"])

(def menu-actions
  (array-map :offices ["Oficinas" "/offices"]
             :users ["Usuarios" "/users"]
             :contacts ["Contactos" "/contacts"]
             :groups ["Grupos" "/groups"]
             :policies ["Reglas" "/policies"]
             :trunks ["Troncales" "/trunks"]
             :notifications ["Notificaciones" "/notifications"]))

(defn- menu-entries
  "Build the menu entries markup from a list of action keywords"
  [actions selected]
  (reduce
   (fn [l entry]
     (if (contains? actions (entry 0))
       (let [menu-data (entry 1)
             style (if (= (entry 0) selected) "selected" "")]
         (conj l [:li {:class style} [:a {:href (menu-data 1)} (menu-data 0)]]))
       l))
   [] menu-actions))

(defn- menu
  "Creates the navigation menu markup"
  [current]
  (let [user-actions (permissions/get-user-actions)]
    (vec (cons :ul (menu-entries user-actions current)))))

(defn button-new
  [text url]
  [:input
   {:type "button"
    :value text
    :class "buttonNew"
    :onclick (format "window.location='%s';" url)}])

(defn common [selection title & content]
  (-> (response/response (html
                          (doctype :html5)
                          [:head
                           [:meta {:charset "utf-8"}]
                           [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
                           [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
                           [:title title]
                           (include-css "/css/amazium.css")
                           (include-css "/css/layout.css")
                           (include-css "/css/formalize.css")
                           (include-css "/css/forms.css")
                           (include-css "/css/jquery.dataTables.css")
                           (include-css "/css/login.css")
                           (include-css "/css/jquery-ui-1.8.13.custom.css")
                           (include-js "/scripts/jquery-1.7.2.min.js")
                           (include-js "/scripts/jquery-ui-1.8.13.custom.min.js")
                           (include-js "/scripts/jquery.dataTables.min.js")
                           (include-js "/scripts/jquery.formalize.min.js")]
                          [:body
                           [:script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;"]
                           (include-js "/scripts/notify-me.js")
                           ;;header
                           [:div {:class "row header"}
                            (header title)]
                           [:div {:class "row content"}
                            [:div {:class "grid_2"} (menu selection)]
                            [:div {:class "grid_10"}
                             [:h2 {:class "container"} title]
                             [:div {:id "content" :class "container"} content]]]
                           [:div {:class "row footer"}
                            (footer)]]))
      (response/header "Cache-Control" "no-store, no-cache, must-revalidate")
      (response/header "Pragma" "no-cache")
      (response/header "Content-Type" "text/html; charset=utf-8")
      (response/header "Expires" "-1")))

(defn four-oh-four []
  "Page not found")

(defn create-entity-table
  [id columns entities actions]
  [:div
   [:table {:id id}
    [:thead
     ;;actions header has no title
     [:tr (map (fn [column] [:th (column 1)]) columns) [:th ""]]]
    [:tbody
     (map (fn [e]
            [:tr
             ;;column fields
             (map (fn
                    [c]
                    [:td (if-let [f (get c 2)]
                           (f e)
                           (get e (c 0)))])
                  columns)
             ;;actions
             [:td
              (map (fn [a] [:a {:href (format (a 0) (:id e))} (a 1)]) actions)]])
          entities)]]
   [:script {:type "text/javascript" :language "javascript"}
    (format "$('#%s').dataTable()" id)]])
