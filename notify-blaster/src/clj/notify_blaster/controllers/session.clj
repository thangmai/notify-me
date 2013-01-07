(ns notify-blaster.controllers.session
  (:require [ring.util.response :as res]
            [notify-blaster.views.session :as view]
            [cemerick.friend :as friend]))


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

(defn show-new
  [req]
  (view view/show-new))