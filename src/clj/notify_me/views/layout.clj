(ns notify-me.views.layout
  (:require [notify-me.models.permissions :as permissions])
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
  [])

(def menu-actions
  {:offices ["Oficinas" "/offices"]
   :users ["Usuarios" "/users"]
   :contacts ["Contactos" "/contacts"]
   :groups ["Grupos" "/groups"]
   :policies ["Reglas" "/policies"]
   :trunks ["Troncales" "/trunks"]
   :notifications ["Notificaciones" "/notifications"]})

(defn- menu-entries
  "Build the menu entries markup from a list of action keywords"
  [actions selected]
  (reduce
   (fn [l a] (let [entry (get menu-actions a)
                   style (if (= a selected) "selected" "")]
               (conj l [:li {:class style} [:a {:href (entry 1)} (entry 0)]])))
   [] actions))

(defn- menu
  "Creates the navigation menu markup"
  [current]
  (let [user-actions (permissions/get-user-actions)]
    (vec (cons :ul (menu-entries user-actions current)))))

(defn common [title & content]
  (html
   (doctype :html5)
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
    [:title title]
    (include-css "/css/base.css")
    (include-css "/css/amazium.css")
    (include-css "/css/layout.css")
    (include-css "/css/formalize.css")
    (include-css "/css/forms.css")
    (include-css "/css/jquery.dataTables.css")
    (include-js "/scripts/jquery-1.7.2.min.js")
    (include-js "/scripts/jquery-ui-1.8.13.custom.min.js")
    (include-js "/scripts/jquery.dataTables.min.js")
    (include-js "/scripts/jquery.formalize.min.js")]
   [:body
    [:script {:type "text/javascript"} "var CLOSURE_NO_DEPS = true;"]
    (include-js "/scripts/notify-me.js")
    ;;header
    [:div {:class "row"}
     (header title)]
    ;;two column content, menu and page body
    [:div {:class "row"}
     [:div {:class "grid_10 offset_2"}
      [:h2 {:class "container"} title]]]
    [:div {:class "row"}
     [:div {:class "grid_2"} (menu :users)]
     [:div {:class "grid_10"}
      [:div {:id "content" :class "container"} content]]]]))

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
             (map (fn [c] [:td (get e (c 0))]) columns)
             ;;actions
             [:td
              (map (fn [a] [:a {:href (format (a 0) (:id e))} (a 1)]) actions)]])
          entities)]]
   [:script {:type "text/javascript" :language "javascript"}
    (format "$('#%s').dataTable()" id)]])
