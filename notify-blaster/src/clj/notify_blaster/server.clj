(ns notify-blaster.server
  (:require [notify-blaster.webserver :as webserver]))

(defn -main []
 (webserver/start []))