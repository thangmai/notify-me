(ns notify-blaster.controllers.notifications
  (:require
   [notify-blaster.models.notification :as model]
   [notify-blaster.models.policy :as policy]
   [notify-blaster.models.contact :as contact]
   [notify-blaster.models.group :as group]
   [notify-blaster.views.notifications :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-blaster.models.validation.notification :as notification-rules])
  (:use
   [notify-blaster.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-blaster.models.validation.core :only [validate *is-unique?*]]
   [notify-blaster.utils])
  (:import java.util.UUID))

(defn all
  "Renders a view with all the defined notifications"
  []
  (view/index (model/all)))

(defn show
  "Renders a notification form read-only"
  [id]
  (when-let [notification (model/one {:id id})]
    (view/render-edit notification)))

(defn show-new
  "Render empty notification form"
  []
  (let [qry {:office_id (current-office-id)}]
    (view/render-new (policy/search qry)
                     (contact/search qry)
                     (group/search qry))))

(defn create!
  "Creates a new notification"
  [params]
  (let [notification (merge params {:office_id (current-office-id)
                                    :id (.toString (java.util.UUID/randomUUID))
                                    :status "CREATED"})]
    (validate notification notification-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (model/create! notification)
                  errors)))))