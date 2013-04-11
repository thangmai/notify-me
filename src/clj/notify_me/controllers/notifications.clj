(ns notify-me.controllers.notifications
  (:refer-clojure :exclude [read-string])
  (:require
   [notify-me.models.notification :as model]
   [notify-me.models.policy :as policy]
   [notify-me.models.contact :as contact]
   [notify-me.models.group :as group]
   [notify-me.models.trunk :as trunk]
   [notify-me.views.notifications :as view]
   [clojure.edn :refer [read-string]]
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
           java.awt.Color
           org.jfree.chart.labels.StandardPieSectionLabelGenerator
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
    (let [attempts (model/attempts notification)]
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

;;nice colors taken from
;;http://stackoverflow.com/questions/1088370/colours-to-piechart-in-jfreechart
(def pie-colors {"CONNECTED" (Color. 169 204 143)
                 "FINISHED" (Color. 181 181 169)
                 "PROCESSING" (Color. 119 157 191)
                 "BUSY" (Color. 255 248 163)
                 "NO ANSWER" (Color. 119 157 191)
                 "FAILED" (Color. 179 0 35)
                 "CANCELLED" (Color. 181 181 169)})

(defmacro create-chart
  [id fn-data key-field value-field]
  `(if-let [notification# (model/one {:id ~id})]
     (let [data# (~fn-data notification#)
           keys# (map ~key-field data#)
           values# (map ~value-field data#)]
       (when (and (seq keys#) (seq values#))
         (let [chart# (incanter.charts/pie-chart keys#
                                                 values#
                                             :legend true)
               label-generator# (StandardPieSectionLabelGenerator. "{1} {0}({2})")]
           (.. chart# getPlot (setLabelGenerator label-generator#))
           (doall (map #(.. chart# getPlot (setSectionPaint (% 0) (% 1))) pie-colors))
           (write-chart chart#))))))

(defn- recipients-chart
  [id]
  (create-chart id model/recipients-summary :last_status :cnt))

(defn- attempts-chart
  [id]
  (create-chart id model/attempts-summary :status :cnt))

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
  (POST "/" request (let [response (create! (read-string (slurp (:body request))))]
                      (if (:status response)
                        (select-keys response [:id]) ;;status name is messing everything up
                        response))) 
  (GET "/:id" [id] (show id)))