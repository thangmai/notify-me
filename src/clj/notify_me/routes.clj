(ns notify-me.routes
  (:require
   [compojure.route :as route]
   [compojure.handler]
   [ring.util.response :as response]
   [cemerick.friend :as friend]
   [notify-me.views.layout :as layout]
   [notify-me.controllers.offices :as offices]
   [notify-me.controllers.users :as users]
   [notify-me.controllers.contacts :as contacts]
   [notify-me.controllers.groups :as groups]
   [notify-me.controllers.policies :as policies]
   [notify-me.controllers.trunks :as trunks]
   [notify-me.controllers.notifications :as notifications]
   [notify-me.controllers.session :as session])
  (:use
     ring.middleware.params
     ring.middleware.keyword-params
     ring.middleware.nested-params
     ring.middleware.session
     notify-me.db-session-store
     [notify-me.auth :only (auth)]
     [compojure.core :only (GET PUT POST ANY defroutes context)]))

(defroutes routes
  ;;files
  (route/resources "/")
  ;;landing
  (GET "/" request
       (response/resource-response "public/index.html"))
  
  ;;controllers
  (context "/offices" request (friend/wrap-authorize offices/routes #{:admin}))
  (context "/users" request (friend/wrap-authorize users/routes #{:user :admin}))
  (context "/contacts" request (friend/wrap-authorize contacts/routes #{:user :admin}))
  (context "/groups" request (friend/wrap-authorize groups/routes #{:user :admin}))
  (context "/policies" request (friend/wrap-authorize policies/routes #{:user :admin}))
  (context "/trunks" request (friend/wrap-authorize trunks/routes #{:user :admin}))
  (context "/notifications" request (friend/wrap-authorize notifications/routes #{:user :admin}))
  
  ;; auth
  (GET "/login" request session/show-new)
  (POST "/login" request session/show-new)
  (friend/logout
   (ANY "/logout" []
        (ring.util.response/redirect "/")))
  ;;404
  (route/not-found (layout/four-oh-four)))

(def app
  (-> routes
      auth
      (wrap-session {:cookie-name "notify-me-session" :store (db-session-store)})
      wrap-keyword-params
      wrap-nested-params
      wrap-params))
