(ns notify-blaster.webserver
  (:require
   [notify-blaster.config :as config]
   [compojure.core :as compojure]
   [ring.adapter.jetty :as jetty]
   [notify-blaster.routes :as routes]))


(defn start
  [opts]
    (prn (str "Starting web server at http://localhost/" (:port config/opts)))
    (jetty/run-jetty routes/app {:join? true :port (:port config/opts)}))
