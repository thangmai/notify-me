(ns notify-me.webserver
  (:require
   [notify-me.config :as config]
   [compojure.core :as compojure]
   [ring.adapter.jetty :as jetty]
   [notify-me.routes :as routes]))


(defn start
  [opts]
    (prn (str "Starting web server at http://localhost/" (:port config/opts)))
    (jetty/run-jetty routes/app {:join? true :port (:port config/opts)}))
