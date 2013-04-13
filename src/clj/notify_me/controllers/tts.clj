(ns notify-me.controllers.tts
  (:require
   [notify-me.config :as config]
   [ring.util.response :as response]
   [clojure.tools.logging :as log])
  (:use
   [clojure.java.shell :only [sh]]
   [compojure.core :only [defroutes GET POST]]))

(defn- translate
  [text]
  (log/info (format "Translating to tts '%s'" text))
  (let [response (sh (:tts-command config/opts) (:temp-dir config/opts) text)
        exit (:exit response)
        filename (clojure.string/trim-newline (:out response))]
    (if (and (= exit 0) (-> filename empty? not))
      (->
       (response/file-response filename {:root (:temp-dir config/opts)})
       (response/content-type "audio/wav")
       (response/header "Content-Disposition" (format "inline; filename='%s'" filename)))
      {:status 500 :body (:err response)})))

(defroutes routes
  (GET ["/:text/translate.wav", :text #"[^/]+"] [text] (translate text)))
