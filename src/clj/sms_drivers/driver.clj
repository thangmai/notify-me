(ns sms-drivers.driver
  (:refer-clojure :exclude [load])
  (:require [notify-me.config :as config]))

;; Dynamically loads the driver to use for sms dispatching
 ;;TODO: this should be configured dynamically somewhere
(def driver-ns 'sms-drivers.ancel)
(require driver-ns)

(defn load
  []
  (let [dr (resolve (symbol (name driver-ns) "load-driver"))]
    (@dr config/sms-opts)))

(defn unload
  "TODO"
  []
  (comment "This should disconnect sms driver if needed"))