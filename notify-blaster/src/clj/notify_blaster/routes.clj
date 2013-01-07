(ns notify-blaster.routes
  (:require
   [compojure.route :as route]
   [compojure.handler]
   [ring.util.response :as response]
   [cemerick.friend :as friend]
   [notify-blaster.views.layout :as layout]
   [notify-blaster.controllers.offices :as offices]
   [notify-blaster.controllers.users :as users]
   [notify-blaster.controllers.session :as session])
  (:use
     ring.middleware.params
     ring.middleware.keyword-params
     ring.middleware.nested-params
     ring.middleware.session
     notify-blaster.db-session-store
     [notify-blaster.auth :only (auth)]
     [compojure.core :only (GET PUT POST ANY defroutes context)]))

(defroutes ^{:private true} user-routes
  (GET "/" [] (users/all))
  (GET "/new" [] (users/show-new))
  (POST "/" {params :params} (users/create! params))
  (GET "/:username" [username] (users/show username))
  (GET "/:username/edit" [username] (users/edit username))
  (POST "/:username" {params :params} (users/update! params)))

(defroutes ^{:private true} office-routes
  (GET "/" [] (offices/all))
  (GET "/new" [] (offices/show-new))
  (POST "/" {params :params} (offices/create! params))
  (GET "/:id" [id] (offices/show id))
  (GET "/:id/edit" [id] (offices/edit id))
  (GET "/:name/unique" [name] (pr-str (offices/is-unique? name)))
  (POST "/:id" {params :params} (offices/update! (:id params) (dissoc params :id))))

(defroutes routes
  ;;files
  (route/resources "/")
  ;;landing
  (GET "/" request
                 (response/resource-response "public/index.html"))
 
  ;;offices
  (context "/offices" request (friend/wrap-authorize office-routes #{:admin}))
  ;; users
  (context "/users" request (friend/wrap-authorize user-routes #{:user :admin}))

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
      (wrap-session {:cookie-name "notify-blaster-session" :store (db-session-store)})
      wrap-keyword-params
      wrap-nested-params
      wrap-params))
