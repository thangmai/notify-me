(ns notify-me.translation
  (:require
   [taoensso.tower :as tower]))

(defn t
  []
  (fn [text & args]
    (let [params (cons (keyword (str "notify-me/" text)) args)]
      (tower/with-locale :en
        (apply tower/t params)))))

