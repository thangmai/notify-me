(ns notify-blaster.client
  (:require-macros [jayq.macros :as jq-macros])
  (:use [jayq.util :only [clj->js]])
  (:require [cljs.reader :as reader]
            [jayq.core :as jq]
            [domina :as d]
            [domina.events :as events]
            [clojure.browser.repl :as repl]))


;;STARTUP
(defn ^:export main []
  (.log js/console "Started"))