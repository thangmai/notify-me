(ns notify-me.controllers.contacts
  (:refer-clojure :exclude [read-string])
  (:require
   [notify-me.models.contact :as model]
   [notify-me.views.contacts :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [clojure.edn :refer [read-string]]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.contact :as contact-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils]))


(defn all
  "Renders a view with all the defined contacts"
  []
  (view/index (model/search {:office_id (current-office-id)})))

(defn show
  "Renders a contact form read-only"
  [id]
  (when-let [contact (model/one {:id (str->int id)})]
    (view/render-form :show contact)))

(defn is-unique?
  ([phone]
     (is-unique? phone nil))
  ([phone contact-id]
     (let [office-id (current-office-id)
           contact (model/one {:cell_phone phone :office_id office-id})]
       (or (nil? contact)
           (= (str (:id contact)) contact-id)))))

(defn edit
  "Renders an edition form for the specified contact"
  [id]
  (when-let [contact (model/one {:id (str->int id)})]
    (view/render-form :edit contact)))

(defn show-new
  "Render empty contact form"
  []
  (view/render-form :new))

(defn- validate-and-save
  [contact callback]
  (binding [*is-unique?* is-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate contact contact-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (callback)
                  errors)))))

(defn create!
  "Creates a new contact"
  [params]
  (let [contact (merge params {:office_id (current-office-id)})]
    (validate-and-save contact (fn [] (model/create! contact)))))

(defn update!
  "Updates an existing contact"
  [params]
  (let [id (:id params)]
    (validate-and-save
     params
     (fn [] (model/update! {:id (str->int id)} (dissoc params :id))))))

(defn delete!
  [id]
  (when-let [contact (model/one {:id (str->int id)})]
    (model/delete! contact)
    (all)))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:phone" [phone] (show phone))
  (GET "/:phone/edit" [phone] (edit phone))
  (GET "/:phone/delete" [phone] (delete! phone))
  (GET "/:phone/unique" [phone] (pr-str (is-unique? phone)))
  (GET "/:id/:phone/unique" [id phone] (pr-str (is-unique? phone id)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))
