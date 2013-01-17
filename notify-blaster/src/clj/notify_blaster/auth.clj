(ns notify-blaster.auth
  (:require [notify-blaster.controllers.users :as users]
            [notify-blaster.models.user :as user]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]
                             [openid :as openid])))


(defn credential-fn
  [email]
  (user/one {:email email}))

(defn session-store-authorize [{:keys [uri request-method params session]}]
  (when (nil? (:cemerick.friend/identity session))
    (if-let [username (get-in session [:cemerick.friend/identity :current])]
      (workflows/make-auth (select-keys (user/one {:username username}) [:id :username])))))

(defn auth
  [ring-app]
  (friend/authenticate
   ring-app
   {:credential-fn (partial creds/bcrypt-credential-fn credential-fn)
    :workflows [(workflows/interactive-form), users/create-new!, session-store-authorize]
    :login-uri "/login"
    :unauthorized-redirect-uri "/login"
    :default-landing-uri "/"}))