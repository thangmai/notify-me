(ns notify-blaster.translation
  (:require
   [taoensso.tower :as tower]))

(defn t
  []
  (fn [text & args]
    (let [params (cons (keyword (str "notify-blaster/" text)) args)]
      (tower/with-locale :en
        (apply tower/t params)))))

