(ns notify-blaster.group
  (:require-macros
   [jayq.macros :as jq-macros])
  (:use
   [notify-blaster.models.validation.core :only [validate *is-unique?*]]
   [jayq.util :only [clj->js]]
   [domina.css :only [sel]])
  (:require
   [notify-blaster.models.validation.group :as rules]
   [jayq.core :as jq]
   [domina :as d]
   [notify-blaster.forms :as f]
   [domina.events :as events]
   [cljs.reader :as reader]))

(defn- is-group-unique?
  "Deferred validator for the group name uniqueness"
  [name group-id]
  (if (and name (> (count name) 0))
    {:deferred (.ajax js/$ (format "%s/unique" name))
     :fn reader/read-string}
    ;;if the field is empty it's going to fail with the :required condition
    true))

(defn- prepare-group
  "Need to send the group id if updating, the assigned contacts gets
   added always as a vector in the :members key"
  [group]
  (let [g (assoc group :members (get-contacts-from-table "#assigned-contacts"))]
    (if (not (is-new?))
      (assoc g :id (get-group-id))
      g)))

(defn- validate-and-save
  "Validates the group is a valid one and submits the form in that case"
  [group]
  (f/remove-errors)
  (binding [*is-unique?* is-group-unique?]
    (validate group rules/rules {}
              (fn [errors]
                (if (empty? errors)
                  (f/post-form "group-form"
                               (prepare-group group))
                  (f/show-errors errors))))))

(defn save-group
  [event]
  (validate-and-save (f/serialize-form "group-form")))

(defn get-group-id
  []
  (d/text (d/by-id "group-id")))

(defn is-new?
  "Returns true if this is not an edition of an existent group"
  []
  (let [id (get-group-id)]
    (= (count id) 0)))


;;Drag and drop support between contact tables

(defn- get-contacts-from-table
  [table]
  (let [data (-> (js/$ table) .dataTable .fnGetData)]
    (doall (map (fn [r]
                  {:id (get r 0)
                   :name (get r 1)
                   :cell_phone (get r 2)
                   :type (get r 3)}) data))))

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
  "Parses a dropped row and builds a contact from it"
  [row]
  {:id (-> (js/$ row) (.find "td:eq(0)") .text)
   :name (-> (js/$ row) (.find "td:eq(1)") .text)
   :cell_phone (-> (js/$ row) (.find "td:eq(2)") .text)
   :type (-> (js/$ row) (.find "td:eq(3)") .text)
   :index (-> (js/$ row) (.find "table") (.attr "index"))
   })

(defn- add-contact
  "Adds a contact object to the specified table"
  [table contact]
  (let [row (clj->js [(:id contact)
                      (:name contact)
                      (:cell_phone contact)
                      (:type contact)])]
        (-> (js/$ table) .dataTable (.fnAddData row))))

(defn- delete-contact
  "Deletes a given contact from a specified table, assumes the contact has an index"
  [table contact]
  (-> (js/$ table) .dataTable (.fnDeleteRow (:index contact))))

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
                                                (let [contact (parse-row (-> ui .-helper (.get 0)))]
                                                  (do
                                                    (add-contact table contact)
                                                    (delete-contact source contact)))))
                               :accept (.-selector source)
                      })))

(defn ^:export main
  []
  (.dataTable (js/$ "#available-contacts"))
  (.dataTable (js/$ "#assigned-contacts"))
  (setup-drag-drop (js/$ "#available-contacts") (js/$ "#assigned-contacts"))
  (setup-drag-drop (js/$ "#assigned-contacts") (js/$ "#available-contacts"))
  (events/listen! (d/by-id "save")
                  :click
                  save-group)
  (events/listen! (d/by-id "cancel")r
                  :click
                  cancel))