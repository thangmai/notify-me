(ns tasks.db
  (:gen-class)
  (:refer-clojure :exclude [alter drop complement
                            bigint boolean char double float time])
  (:require [notify-me.models.user :as user]
            [notify-me.models.sms_provider :as sms_provider])
  (:use (lobos core connectivity)))


(defn rebuild
  []
  (binding [lobos.migration/*src-directory* "src/clj"]
    (rollback :all)
    (migrate)))

(defn seed
  []
  (println "Creating root user")
  (user/create! {:username     "admin"
                 :email        "admin@presidencia.gub.uy"
                 :display_name "admin"
                 :password     "sinclave"
                 :roles [:admin]})
  (println "Creating Ancel SMS provider")
  (sms_provider/create! {:name "Ancel SMS Empresa"
                         :provider "Ancel.SMSEmpresa"}))

(defn -main
  [task-name]
  (condp = task-name
    "rebuild" (rebuild)
    "seed" (seed)))
