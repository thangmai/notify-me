(ns notify-blaster.routes
  (:require
   [compojure.route :as route]
   [compojure.handler]
   [ring.util.response :as response]
   [cemerick.friend :as friend]
   [notify-blaster.views.layout :as layout]
   [notify-blaster.controllers.offices :as offices]
   [notify-blaster.controllers.users :as users]
   [notify-blaster.controllers.contacts :as contacts]
   [notify-blaster.controllers.groups :as groups]
   [notify-blaster.controllers.policies :as policies]
   [notify-blaster.controllers.notifications :as notifications]
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
  (GET "/:name/unique" [name] (pr-str (users/is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (users/is-unique? name id)))
  (POST "/:id" {params :params} (users/update! (:id params) params)))

(defroutes ^{:private true} contact-routes
  (GET "/" [] (contacts/all))
  (GET "/new" [] (contacts/show-new))
  (POST "/" {params :params} (contacts/create! params))
  (GET "/:phone" [phone] (contacts/show phone))
  (GET "/:phone/edit" [phone] (contacts/edit phone))
  (GET "/:phone/unique" [phone] (pr-str (contacts/is-unique? phone)))
  (GET "/:id/:phone/unique" [id phone] (pr-str (contacts/is-unique? phone id)))
  (POST "/:id" {params :params} (contacts/update! (:id params) params)))


(defroutes ^{:private true} office-routes
  (GET "/" [] (offices/all))
  (GET "/new" [] (offices/show-new))
  (POST "/" {params :params} (offices/create! params))
  (GET "/:id" [id] (offices/show id))
  (GET "/:id/edit" [id] (offices/edit id))
  (GET "/:name/unique" [name] (pr-str (offices/is-office-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (offices/is-office-unique? id name)))
  (POST "/:id" {params :params} (offices/update! (:id params) params)))


(defroutes ^{:private true} group-routes
  (GET "/" [] (groups/all))
  (GET "/new" [] (groups/show-new))
  (POST "/" request (groups/create! (read-string (slurp (:body request)))))
  (GET "/:name" [name] (groups/show name))
  (GET "/:name/edit" [name] (groups/edit name))
  (GET "/:name/unique" [name] (pr-str (groups/is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (groups/is-unique? name id)))
  (POST "/:id" request (groups/update! (read-string (slurp (:body request))))))


(defroutes ^{:private true} policy-routes
  (GET "/" [] (policies/all))
  (GET "/new" [] (policies/show-new))
  (POST "/" request (policies/create! (read-string (slurp (:body request)))))
  (GET "/:id" [id] (policies/show id))
  (GET "/:id/edit" [id] (policies/edit id))
  (GET "/:name/unique" [name] (pr-str (policies/is-unique? name)))
  (GET "/:id/:name/unique" [id name] (pr-str (policies/is-unique? name id)))
  (POST "/:id" request (policies/update! (read-string (slurp (:body request))))))


(defroutes ^{:private true} notification-routes
  (GET "/" [] (notifications/all))
  (GET "/new" [] (notifications/show-new))
  (POST "/" request (notifications/create! (read-string (slurp (:body request)))))
  (GET "/:id" [id] (notifications/show id)))

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
  ;; contacts
  (context "/contacts" request (friend/wrap-authorize contact-routes #{:user :admin}))
  ;;groups
  (context "/groups" request (friend/wrap-authorize group-routes #{:user :admin}))
  ;;policies
  (context "/policies" request (friend/wrap-authorize policy-routes #{:user :admin}))
  ;;notifications
  (context "/notifications" request (friend/wrap-authorize notification-routes #{:user :admin}))

  
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
