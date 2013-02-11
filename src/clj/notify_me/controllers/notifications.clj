(ns notify-me.controllers.notifications
  (:require
   [notify-me.models.notification :as model]
   [notify-me.models.policy :as policy]
   [notify-me.models.contact :as contact]
   [notify-me.models.group :as group]
   [notify-me.models.trunk :as trunk]
   [notify-me.views.notifications :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.notification :as notification-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils])
  (:import java.util.UUID))

(defn all
  "Renders a view with all the defined notifications"
  []
  (view/index (model/search {:office_id (current-office-id)})))

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
                     (trunk/search qry)
                     (contact/search qry)
                     (group/search qry))))

(defn validate-and-save
  [notification callback]
  (validate notification notification-rules/rules {}
            (fn [errors]
              (if (empty? errors)
                (callback)
                errors))))

(defn create!
  "Creates a new notification"
  [params]
  (let [notification (merge params {:office_id (current-office-id)
                                    :id (.toString (java.util.UUID/randomUUID))
                                    :status "CREATED"})]
    (validate-and-save notification (fn [] (model/create! notification)))))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  ;;status field from the newly created notification gets confused
  ;;with http status result and fails
  (POST "/" request (dissoc (create! (read-string (slurp (:body request)))) :status)) 
  (GET "/:id" [id] (show id)))