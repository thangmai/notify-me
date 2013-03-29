(ns notify-me.webserver
  (:require
   [notify-me.config :as config]
   [compojure.core :as compojure]
   [ring.adapter.jetty :as jetty]
   [notify-me.routes :as routes]))


(defn start
  [opts]
    (jetty/run-jetty routes/app {:join? true :port (:port config/opts)}))
