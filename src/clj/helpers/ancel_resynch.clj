(ns helpers.ancel-resynch
  (:gen-class)
  (:require [sms-drivers.ancel :as ancel]
            [notify-me.config :as config]
            [notify-me.models.contact :as contact]
            [notify-me.models.group :as group]
            [clojure.tools.logging :as log]))

(defn register-contacts
  []
  (let [contacts (contact/all)]
    (doseq [c contacts]
      (log/info "Registering contact" (:cell_phone c))
      (ancel/register-phone-number identity c))))

(defn register-groups
  []
  (let [groups (group/all)]
    (doseq [g groups]
      (log/info "Registering group" (:name g))
      (ancel/register-group identity g))))

(defn -main 
  [& args]
  (log/info "Resynch ancel stuff")
  (ancel/load-driver config/sms-opts)
  (register-contacts)
  (register-groups))
