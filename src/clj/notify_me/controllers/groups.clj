(ns notify-me.controllers.groups
  (:refer-clojure :exclude [read-string])
  (:require
   [notify-me.models.group :as model]
   [notify-me.models.contact :as contact]
   [notify-me.views.groups :as view]
   [clojure.edn :refer [read-string]]
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
  (view/index (model/search {:office_id (current-office-id)})))

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

(defn delete!
  [id]
  (when-let [group (model/one {:id (str->int id)})]
    (model/delete! group)
    (all)))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:name" [name] (show name))
  (GET "/:name/edit" [name] (edit name))
  (GET "/:name/delete" [name] (delete! name))
  (GET "/:name/unique" [name] (pr-str (is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (is-unique? name id)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))