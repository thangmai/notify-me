(ns notify-me.webserver
  (:require
   [notify-me.config :as config]
   [ring.adapter.jetty :as jetty]
   [notify-me.routes :as routes]))

(def server (atom nil))

(defn stop []
  (when @server
    (.stop @server)))

(defn start []
  (stop)
  (when-let [s (jetty/run-jetty routes/app 
                                {:join? false :port (:port config/opts)})]
    (reset! server s)))
