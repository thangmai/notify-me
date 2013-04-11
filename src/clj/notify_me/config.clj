(ns notify-me.config
  (:require [clojure.java.io]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def default-opts {:port 5000
                   :host "localhost"
                   :user "user"
                   :password "password"
                   :temp-dir "/tmp"
                   :tts-command "text2mp3.sh"})

(defn read-properties
  [file-name]
  (try+ 
   (with-open [^java.io.Reader reader (clojure.java.io/reader file-name)] 
     (let [props (java.util.Properties.)]
       (.load props reader)
       (into {} (for [[k v] props] [(keyword k) (read-string v)]))))
   (catch Object _
     (log/warn "Unable to read properties file" file-name)
     {})))

(def opts (merge default-opts (read-properties "/etc/notify-me.conf")))