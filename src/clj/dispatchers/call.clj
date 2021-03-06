(ns dispatchers.call
  (:require [notify-me.jobs.dispatcher :as dispatcher]
            [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events]
            [dispatchers.model :as model]
            [clojure.tools.logging :as log])
  (:import java.util.UUID))

;; Signal the end of the call to the waiting promise in order to
;; release the channel
(defmethod events/handle-event "Hangup"
  [event context]
  (log/debug "Hangup event received: " (pr-str event))
  (manager/with-connection context
    (let [unique-id (:Uniqueid event)
          call-id (manager/get-user-data unique-id)
          prom (manager/get-user-data call-id)]
      (if call-id
        (do
          (log/info (format "Hanging up call %s with unique id %s" call-id unique-id))
          (deliver prom event)
          (manager/remove-user-data! call-id) ;;FIX: this should be done
          ;;on the waiting side or promise may get lost
          (manager/remove-user-data! unique-id))
        (log/error (format "Hanging up call with unique id %s no call found" unique-id))))))

;; When CALLID is set, relate it to the call unique-id
;; to be used later in hangup detection
;;
;; The context has the following info inside:
;;   callid => promise
;;   Uniqueid => callid
;;
;; so it's possible to deliver a response to someone waiting
;; on the callid promise
(defmethod events/handle-event "VarSet"
  [event context]
  (when (= (:Variable event) "CALLID")
    (manager/with-connection context
      (log/debug (format "Setting data %s match %s" (:Uniqueid event) (:Value event)))
      (manager/set-user-data! (:Uniqueid event) (:Value event)))))

(defmethod events/handle-event :default
  [event context]
  (log/debug (format "Unhandled Event => %s" event)))

(defmethod model/normal-clearing? "CALL"
  [notification result]
  (= "4" (:Reason result)))

(defmethod model/get-cause-name "CALL"
  [notification result]
  (if (= (:type result) :clj-asterisk.internal.core/timeout)
    "NO ANSWER"
    (case (:Reason result)
      "5" "BUSY"
      "8" "BUSY"
      "3" "NO ANSWER"
      "4" "CONNECTED"
      "0" "FAILED"
      "FAILED")))

(defn call
  "Call a contact and wait till the call ends.
   Function returns the hangup event or nil if timedout"
  [context notification contact]
  (log/info "Dialing contact" (:address contact) "for notification" (:id notification))
  (manager/with-connection context
    (let [trunk (model/get-trunk notification)
          policies (model/get-policies notification)
          timeout-secs (or (:no_answer_timeout policies) 30)
          _ (log/info "Timeout for" (:address contact) "is" timeout-secs)
          call-id (.toString (java.util.UUID/randomUUID))
          prom (manager/set-user-data! call-id (promise))
          contact-address (str (:prefix trunk) (:address contact))
          response (manager/action :Originate
                                   {:Channel (format "%s/%s/%s"
                                                     (:technology trunk)
                                                     (:number trunk)
                                                     contact-address)
                                    :Context (:context trunk)
                                    :Exten (:extension trunk)
                                    :Priority (:priority trunk)
                                    :Timeout (* timeout-secs 1000)
                                    :CallerID (:callerid trunk)
                                    :Async "true"
                                    :Variables [(format "MESSAGE=%s" (:message notification))
                                                (format "CALLID=%s" call-id)]})]
      (log/info "Dial returned" (:Response response) ":" (:Reason response) 
                "for contact" (:address contact) "on notification" (:id notification))
      (model/save-result notification
                         contact
                         (assoc response :Cause (:Reason response))))))

(defn dispatch-calls
  "Returns the list of futures of each call thread (one p/contact)"
  [context notification contacts]
  (map #(future (call context notification %)) contacts))

(defn get-available-ports
  "Returns the notification number of available ports to dial on"
  [notification]
  (let [out-trunk (model/get-trunk notification)]
    (:capacity out-trunk)))

(defn process
  "Loops until all contacts for a notification are reached or finally
   cancelled"
  [notification context]
  (log/info "Processing CALL notification " (:id notification))
  (when (not (model/cancelling? notification))
    (model/update-status! notification "RUNNING"))
  (let [total-ports (get-available-ports notification)
        contact-list (-> (model/retrieve-rcpt notification)
                         (model/expand-rcpt notification))]
    (loop [remaining contact-list pending-contacts []]
      (cond
       (model/cancelling? notification)
             (do
              (model/update-status! notification "STOPPING")
              (let [joined-recipients (map (fn [p] @p) pending-contacts)]
                (model/update-status! notification "STOPPED")))
       (or (seq remaining)
           (seq pending-contacts))
             (do
               (let [pending (filter (comp not realized?) pending-contacts)
                     finished (filter realized? pending-contacts)
                     failed (filter
                             (fn [r] (not (contains? #{"CONNECTED" "CANCELLED"} (:status @r))))
                             finished)
                     free-ports (- total-ports (count pending))
                     contacts (take free-ports remaining)
                     dialing (dispatch-calls context notification contacts)]
                 (log/debug (format "Pending %s Finished %s Failed %s Free Ports %s Dispatched %s"
                                  (count pending) (count finished) (count failed) free-ports
                                  (count dialing)))
                 (Thread/sleep 1000)
                 (recur (concat (drop free-ports remaining) (map (fn [r] (:contact @r)) failed))
                        (concat pending dialing))))))
    (model/update-status! notification "FINISHED")))

(defmethod dispatcher/dispatch "CALL"
  [notification]
  (let [trunk (model/get-trunk notification)] 
    (when (model/is-trunk-free? notification)
      (log/info (format "Notification %s, connecting to asterisk at %s" (:id notification) (:host trunk)))
      (manager/with-config {:name (:host trunk)}
        (if-let [context (manager/login (:user trunk) (:password trunk) :with-events)]
          (manager/with-connection context
            (process notification context)
            (manager/logout))
          (do (when (model/cancelling? notification)
                (model/update-status! notification "FINISHED"))
              (log/error (format "Unable to login to host %s for notification %s" (:host trunk) (:id notification))))))))
  (log/info "Finishing dispatch for " (:id notification)))
