(ns notify-me.controllers.policies
  (:refer-clojure :exclude [read-string])
  (:require
   [notify-me.models.policy :as model]
   [notify-me.views.policies :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [clojure.edn :refer [read-string]]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.policy :as policy-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils]))

(defn all
  "Renders a view with all the defined policies"
  []
  (view/index (model/search {:office_id (current-office-id)})))

(defn show
  "Renders a p form read-only"
  [id]
  (when-let [policy (model/one {:id (str->int id)})]
    (view/render-form :show policy)))

(defn is-unique?
  ([name]
     (is-unique? name nil))
  ([name policy-id]
     (let [office-id (current-office-id)
           policy (model/one {:name name :office_id office-id})]
       (or (nil? policy)
           (= (str (:id policy)) policy-id)))))

(defn edit
  "Renders an edition form for the specified policy"
  [id]
  (when-let [policy (model/one {:id (str->int id)})]
    (view/render-form :edit policy)))

(defn show-new
  "Render empty policy form"
  []
  (view/render-form :new))

(defn- validate-and-save
  [policy callback]
  (binding [*is-unique?* is-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate policy policy-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (callback)
                  errors)))))

(defn create!
  "Creates a new policy"
  [params]
  (let [policy (merge params {:office_id (current-office-id)})]
    (validate-and-save policy (fn [] (model/create! policy)))))

(defn update!
  "Updates an existing policy"
  [params]
  (let [id (:id params)
        policy (merge params {:office_id (current-office-id)})]
    (validate-and-save policy (fn [] (model/update! {:id (str->int id)} (dissoc policy :id))))))

(defn delete!
  [id]
  (when-let [policy (model/one {:id (str->int id)})]
    (model/delete! policy)
    (all)))

(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:id" [id] (show id))
  (GET "/:id/edit" [id] (edit id))
  (GET "/:id/delete" [id] (delete! id))
  (GET "/:name/unique" [name] (pr-str (is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (is-unique? name id)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))
