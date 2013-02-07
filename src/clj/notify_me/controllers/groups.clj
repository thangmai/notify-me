(ns notify-me.controllers.groups
  (:require
   [notify-me.models.group :as model]
   [notify-me.models.contact :as contact]
   [notify-me.views.groups :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.group :as group-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils]))


(defn- is-member?
  "Returns nil if the specified contact-id does not belong to the contact-list"
  [contact-id member-list]
  (first (filter #(= (:id %) contact-id) member-list)))

(defn available-contacts
  "Set difference between current group contacts
   and all available ones for the current office"
  [group]
  (let [all (contact/search {:office_id (current-office-id)})
        members (:members group)]
    (filter #(not (is-member? (:id %) members)) all)))

(defn all
  "Renders a view with all the defined contacts"
  []
  (view/index (model/all)))

(defn show
  "Renders a contact form read-only"
  [id]
  (when-let [group (model/one {:id (str->int id)})]
    (view/render-form :show group (available-contacts group))))

(defn is-unique?
  ([name]
     (is-unique? name nil))
  ([name group-id]
     (let [office-id (current-office-id)
           group (model/one {:name name :office_id office-id})]
       (or (nil? group)
           (= (str (:id group)) group-id)))))

(defn edit
  "Renders an edition form for the specified group"
  [id]
  (when-let [group (model/one {:id (str->int id)})]
    (view/render-form :edit group (available-contacts group))))

(defn show-new
  "Render empty group form"
  []
  (view/render-form :new nil (available-contacts nil)))

(defn- validate-and-save
  [group callback]
  (binding [*is-unique?* is-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate group group-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (callback)
                  errors)))))


(defn create!
  "Creates a new group"
  [params]
  (let [group (merge params {:office_id (current-office-id)})]
    (validate-and-save group (fn [] (model/create! group)))))

(defn update!
  "Updates an existing group"
  [params]
  (let [id (:id params)
        group (merge params {:office_id (current-office-id)})]
    (validate-and-save group (fn [] (model/update! {:id (str->int id)} group)))))
