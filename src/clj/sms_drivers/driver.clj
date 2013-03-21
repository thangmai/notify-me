(ns sms-drivers.driver
  (:refer-clojure :exclude [load]))

;; Dynamically loads the driver to use for sms dispatching
 ;;TODO: this should be configured dynamically somewhere
(def driver-ns 'sms-drivers.ancel)
(require driver-ns)

(defn load
  []
  (resolve driver-ns 'driver))