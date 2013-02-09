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


(defn- validate-and-save
  "Validates the group is a valid one and submits the form in that case"
  [notification]
  (f/remove-errors)
  (validate notification rules/rules {}
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
  [table event]
  (let [tr (-> (js/$ (.-target event)) (.closest "tr"))
        position (-> (js/$ table) .dataTable (.fnGetPosition (.get tr 0)))
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
  [table recipient]
  (-> (js/$ table) .dataTable (.fnDeleteRow (:index recipient))))

(defn setup-drag-drop
  "Setups drag and drop between a source and target tables"
  [source target]
  (.draggable source (clj->js {
                               :helper (fn [event] (draggable-row source event))
                               :appendTo "body"
                      }))
  (.droppable target (clj->js {
                               :drop (fn [event ui]
                                       (this-as table
                                                (let [recipient (parse-row (-> ui .-helper (.get 0)))]
                                                  (do
                                                    (add-recipient table recipient)
                                                    (delete-recipient source recipient)))))
                               :accept (.-selector source)
                      })))



(defn on-type-changed
  [event]
  (let [type (d/value (d/by-id "type"))
        trunk-row (.-parentNode (.-parentNode (d/by-id "trunk_id")))]
    (case type
      "CALL" (d/set-styles! trunk-row {:display "table-row"})
      "SMS" (d/set-styles! trunk-row {:display "none"}))))

(defn ^:export main
  []
  (.dataTable (js/$ "#available-recipients"))
  (.dataTable (js/$ "#assigned-recipients"))
  (setup-drag-drop (js/$ "#available-recipients") (js/$ "#assigned-recipients"))
  (setup-drag-drop (js/$ "#assigned-recipients") (js/$ "#available-recipients"))
  (events/listen! (d/by-id "save")
                  :click
                  save-notification)
  (events/listen! (d/by-id "cancel")
                  :click
                  cancel)
  (events/listen! (d/by-id "type")
                  :change
                  on-type-changed))
