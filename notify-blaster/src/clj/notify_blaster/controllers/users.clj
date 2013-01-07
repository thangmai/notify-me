(ns notify-blaster.controllers.users
  (:require [notify-blaster.models.user :as user]
            [notify-blaster.views.users :as view]
            [ring.util.response :as res]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows])
  (:use notify-blaster.models.permissions))

(defmacro view
  "provides defaults for the map provided to view functions and allows
  you to provide additional key value pairs. Assumes that a variable
  named req exists"

  [view-fn & keys]
  `(let [x# {:current-auth (friend/current-authentication)
             :errors {}
             :params (:params ~'req)
             :req ~'req}]
     (~view-fn (into x# (map vec (partition 2 ~(vec keys)))))))

(defmacro if-valid
  [to-validate validations errors-name & then-else]
  `(let [to-validate# ~to-validate
         validations# ~validations
         ~errors-name (= to-validate# validations#)]
     (if (empty? ~errors-name)
       ~(first then-else)
       ~(second then-else))))


(defn all
  []
  (user/all))

(defn show-new
  [req]
  (view view/show-new))

(defn user-from-req
  [req]
  (user/for-user-page (:params req)))

(defn create!
  [req]
  (let [{:keys [uri request-method params]} req]
    (when (and (= uri "/users")
               (= request-method :post))
      (if-valid
       params
       (:create user/validation-contexts)
       errors
       
       (workflows/make-auth (user/create! params))
       {:body (view view/show-new :errors errors)}))))

(defn show
  [req]
  (view
   view/show
   :user (user-from-req req)))

(defn edit
  [req]
  (let [username (get-in req [:params :username])]
    (protect
     (can-modify-profile? username)
     (view
      view/edit
      :user (user/one {:username username})))))

;; TODO don't really need to have a redirect here do I?
(defn update!
  [req]
  (let [params   (:params req)
        username (:username params)]
    (protect
     (can-modify-profile? username)
     (let [validations (cond
                        (:change-password params)
                        ((:change-password user/validation-contexts)
                         (let [user (user/one {:username username})] (:password user)))
                        
                        (:email params)
                        (:update-email user/validation-contexts)
                        
                        :else {})]
       
       (if-valid
        params validations errors
        (let [new-attributes (if (:change-password params)
                               {:password (get-in params [:change-password :new-password])}
                               (dissoc params :username))]
          (user/update! {:username username} new-attributes)
          (res/redirect (str "/users/" username "/edit?success=true")))
        (view
         view/edit
         :user params
         :errors errors))))))