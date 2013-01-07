(ns tasks.db
  (:refer-clojure :exclude [alter drop complement
                            bigint boolean char double float time])
  (:require [notify-blaster.models.user :as user])
  (:use (lobos core connectivity)))


(defn rebuild
  []
  (binding [lobos.migration/*src-directory* "src/clj"]
    (rollback :all)
    (migrate)))

(defn seed
  []
  (println (user/create! {:username     "admin"
                          :email        "guillermo.winkler@gmail.com"
                          :display_name "admin"
                          :password     "sinclave"
                          :roles "[:admin]"})))
(defn -main
  [task-name]
  (condp = task-name
    "rebuild" (rebuild)
    "seed" (seed)))