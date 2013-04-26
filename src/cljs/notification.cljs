(ns notify-me.notification
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-me.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-me.models.validation.notification :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-me.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))


(defn- get-rules
  []
  (let [limit (d/attr (d/by-id "message-counter") :sms-limit)
        is-sms? (= "SMS" (d/value (d/by-id "type")))]
    (if (and is-sms? limit (>= (js/parseInt limit) 0))
      (assoc-in 
       (assoc-in rules/rules [:message :validations :max-length] (js/parseInt limit))
       [:message :messages :max-length] (format "El mensaje no puede superar los %s caracteres" limit))
      rules/rules)))

(defn- validate-and-save
  "Validates the group is a valid one and submits the form in that case"
  [notification]
  (f/remove-errors)
  (validate notification (get-rules) {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "notification-form" notification)
                  (f/show-errors errors)))))

(defn assoc-recipients
  [notification]
  (assoc notification :members (get-recipients-from-table "#assigned-recipients")))

(defn save-notification
  [event]
  (let [notification (f/serialize-form "notification-form")]
    (validate-and-save (assoc-recipients notification))))

;;Drag and drop support between recipient tables

(defn- get-recipients-from-table
  [table]
  (let [data (-> (js/$ table) .dataTable .fnGetData)]
    (doall (map (fn [r]
                  {:id (get r 0)
                   :name (get r 1)
                   :type (get r 2)}) data))))

(defn draggable-row
  "Creates a draggable row when a drag starts"
  [drag-row event]
  (let [tr (-> (js/$ (.-target event)) (.closest "tr"))
        position (-> (js/$ (.-parent (.-parent drag-row))) .dataTable (.fnGetPosition (.get tr 0)))
        row (-> (js/$ "<div class='drag-row'><table></table></div>")
                (.find "table")
                (.append (.clone tr))
                (.attr "index" position)
                .end)]
    row))

(defn- parse-row
  "Parses a dropped row and builds a recipient from it"
  [row]
  {:id (-> (js/$ row) (.find "td:eq(0)") .text)
   :name (-> (js/$ row) (.find "td:eq(1)") .text)
   :type (-> (js/$ row) (.find "td:eq(2)") .text)
   :index (-> (js/$ row) (.find "table") (.attr "index"))
   })

(defn- add-recipient
  "Adds a recipient object to the specified table"
  [table recipient]
  (let [row (clj->js [(:id recipient)
                      (:name recipient)
                      (:type recipient)])]
        (-> (js/$ table) .dataTable (.fnAddData row))))

(defn- delete-recipient
  "Deletes a given recipient from a specified table, assumes the recipient has an index"
  [drag-row recipient]
  (-> (js/$ (.parent (.parent drag-row))) .dataTable (.fnDeleteRow (:index recipient))))

(defn setup-drag-drop
  "Setups drag and drop between a source and target tables"
  [source target]
  (.draggable source (clj->js {
                               :helper (fn [event] (draggable-row source event))
                               :appendTo "body"}))
  (.droppable target (clj->js {
                               :drop (fn [event ui]
                                       (this-as table
                                                (let [recipient (parse-row (-> ui .-helper (.get 0)))]
                                                  (do
                                                    (add-recipient table recipient)
                                                    (delete-recipient source recipient)))))
                               :accept (.-selector source)})))

(defn display-sms-limit
  []
  (let [counter-label (d/by-id "message-counter")
        limit (d/attr counter-label :sms-limit)]
    (when (and limit (> (js/parseInt limit) 0))
      (let [length (count (d/value (d/by-id "message")))
            remains (- (js/parseInt limit) length)]
        (d/set-text! counter-label remains)
        (if (>= remains 0)
          (-> counter-label (d/add-class! "on-limit") (d/remove-class! "off-limit"))
          (-> counter-label (d/add-class! "off-limit") (d/remove-class! "on-limit")))))))

(defn clear-sms-limit
  []
  (d/set-text! (d/by-id "message-counter") ""))

(defn on-type-changed
  [event]
  (let [type (d/value (d/by-id "type"))
        trunk-row (.-parentNode (.-parentNode (d/by-id "trunk_id")))]
    (case type
      "CALL" (do 
               (d/set-styles! trunk-row {:display "table-row"})
               (clear-sms-limit))
      "SMS" (do 
              (d/set-styles! trunk-row {:display "none"})
              (display-sms-limit)))))

(defn on-message-changed
  [event]
  (when (= "SMS" (d/value (d/by-id "type")))
    (display-sms-limit)))

(defn tts-url
  []
  (let [message (d/value (d/by-id "message"))]
    (format "/tts/%s/translate" message)))

(defn play-tts
  [event]
  (d/set-attr! (d/by-id "play-tts") "a" (tts-url)))

(defn drag-and-drop-bindings
  []
  (setup-drag-drop (js/$ "#available-recipients tr") (js/$ "#assigned-recipients"))
  (setup-drag-drop (js/$ "#assigned-recipients tr") (js/$ "#available-recipients")))

(defn ^:export main
  []
  (.dataTable (js/$ "#available-recipients") (clj->js {:fnDrawCallback drag-and-drop-bindings}))
  (.dataTable (js/$ "#assigned-recipients") (clj->js {:fnDrawCallback drag-and-drop-bindings}))
  (drag-and-drop-bindings)
  (events/listen! (d/by-id "save")
                  :click
                  save-notification)
  (events/listen! (d/by-id "cancel")
                  :click
                  f/back)
  (events/listen! (d/by-id "message")
                  :keyup
                  on-message-changed)
  (events/listen! (d/by-id "type")
                  :change
                  on-type-changed)
  (events/listen! (d/by-id "play-tts")
                  :click
                  play-tts))
