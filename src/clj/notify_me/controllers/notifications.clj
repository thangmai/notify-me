(ns notify-me.controllers.notifications
  (:require
   [notify-me.models.notification :as model]
   [notify-me.models.policy :as policy]
   [notify-me.models.contact :as contact]
   [notify-me.models.group :as group]
   [notify-me.models.trunk :as trunk]
   [notify-me.views.notifications :as view]
   [incanter core stats charts]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.notification :as notification-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils])
  (:import java.util.UUID
           (java.io ByteArrayOutputStream
                    ByteArrayInputStream)))

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

(defn- show-detail
  [id]
  (if-let [notification (model/one {:id id})]
    (let [attempts (model/attempts notification)
          _ (println attempts)]
      (view/render-dashboard notification attempts))
    {:status 404 :body "not found"}))

(defn- write-chart
  [chart]
  (let [out-stream (ByteArrayOutputStream.)
        in-stream (do
                    (incanter.core/save chart out-stream)
                    (ByteArrayInputStream.
                     (.toByteArray out-stream)))]
    (-> (res/response in-stream)
        (res/content-type "image/png"))))

(defn- recipients-chart
  [id]
  (let [chart (incanter.charts/pie-chart ["a" "b" "c"]
                                         [10 20 30])]
    (write-chart chart)))

(defn multi-series-chart
  "Creates a xy-chart with multiple series extracted from column data
  as specified by series parameter"
  [{:keys [series title x-label y-label data]}]
  (let [chart (incanter.charts/time-series-plot :Date (first series)
                                                 :x-label x-label
                                                 :y-label y-label
                                                 :title title
                                                 :series-label (first series)
                                                 :legend true
                                                 :data data)]
    (reduce #(incanter.charts/add-lines %1 :Date %2 :series-label %2 :data data) chart (rest series))))

(defn- attempts-chart
  [id]
  (comment (let [chart (multi-series-chart {:series symbols
                                   :x-label "Date"
                                   :y-label "Return"
                                   :title "Accumulated Daily Returns"
                                   :data (acc-daily-rets adj-close-data)})]))
  (recipients-chart "1"))

(defn- cancel-notification
  [id])

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (GET "/:id/view" [id] (show-detail id))
  (GET "/:id/cancel" [id] (cancel-notification id))
  (GET "/:id/rcpt-chart" [id] (recipients-chart id))
  (GET "/:id/attempts-chart" [id] (attempts-chart id))
  ;;status field from the newly created notification gets confused
  ;;with http status result and fails
  (POST "/" request (dissoc (create! (read-string (slurp (:body request)))) :status)) 
  (GET "/:id" [id] (show id)))