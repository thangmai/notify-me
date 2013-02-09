(ns notify-me.controllers.trunks
  (:require
   [notify-me.models.trunk :as model]
   [notify-me.views.trunks :as view]
   [ring.util.response :as res]
   [cemerick.friend :as friend]
   [cemerick.friend.workflows :as workflows]
   [notify-me.models.validation.trunk :as trunk-rules])
  (:use
   [notify-me.models.permissions]
   [compojure.core :only [defroutes GET POST]]
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [notify-me.utils]))

(defn all
  "Renders a view with all the defined trunks"
  []
  (view/index (model/all)))

(defn show
  "Renders a trunk form read-only"
  [id]
  (when-let [trunk (model/one {:id (str->int id)})]
    (view/render-form :show trunk)))

(defn is-unique?
  ([name]
     (is-unique? name nil))
  ([name trunk-id]
     (let [office-id (current-office-id)
           trunk (model/one {:name name :office_id office-id})]
       (or (nil? trunk)
           (= (str (:id trunk)) trunk-id)))))

(defn edit
  "Renders an edition form for the specified trunk"
  [id]
  (when-let [trunk (model/one {:id (str->int id)})]
    (view/render-form :edit trunk)))

(defn show-new
  "Render empty trunk form"
  []
  (view/render-form :new))

(defn- validate-and-save
  [trunk callback]
  (binding [*is-unique?* is-unique?]
    ;;TODO: this should account for deferred validations and wait on
    ;; the response, now assumes callback is called on the same thread
    (validate trunk trunk-rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (callback)
                  errors)))))

(defn create!
  "Creates a new trunk"
  [params]
  (let [trunk (merge params {:office_id (current-office-id)})]
    (validate-and-save trunk (fn [] (model/create! trunk)))))

(defn update!
  "Updates an existing trunk"
  [params]
  (let [id (:id params)
        trunk (merge params {:office_id (current-office-id)})]
    (validate-and-save trunk (fn [] (model/update! {:id (str->int id)} (dissoc trunk :id))))))


(defroutes routes
  (GET "/" [] (all))
  (GET "/new" [] (show-new))
  (POST "/" request (create! (read-string (slurp (:body request)))))
  (GET "/:id" [id] (show id))
  (GET "/:id/edit" [id] (edit id))
  (GET "/:name/unique" [name] (pr-str (is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (is-unique? name id)))
  (POST "/:id" request (update! (read-string (slurp (:body request))))))
