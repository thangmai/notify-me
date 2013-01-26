(ns dispatchers.call
  (:require [clj-asterisk.manager :as manager]
            [clj-asterisk.events :as events]
            [clojure.java.jdbc :as sql])
  (:import java.util.UUID))

(def ^:dynamic *database-url* "postgresql://localhost/notify-blaster?user=guille&password=bogan731")

;; Signal the end of the call to the waiting promise in order to
;; release the channel
(defmethod events/handle-event "Hangup"
  [event context]
  (manager/with-connection context
    (let [unique-id (:Uniqueid event)
          call-id (manager/get-user-data unique-id)
          prom (manager/get-user-data call-id)]
      (deliver prom event)
      (manager/remove-user-data! call-id) ;;FIX: this should be done
      ;;on the waiting side or promise may get lost
      (manager/remove-user-data! unique-id))))

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
      (manager/set-user-data! (:Uniqueid event) (:Value event)))))

(defn retrieve-rcpt
  "Retrieves the pending list of recipients for a notification
   and flags them as being processed"
  [notification]
  (sql/with-connection *database-url*
    (sql/with-query-results results
      [(format "UPDATE notification_recipient
                SET last_status = 'PROCESSING'
                WHERE notification = '%s' AND
                      (last_status NOT IN ('CANCELLED', 'FINISHED') OR
                       last_status IS NULL)
                RETURNING *" (:id notification))]
      (into [] results))))

(defn contact-rcpt
  "Get all direct contacts assigned to a notification"
  [notification]
  (sql/with-query-results results
    [(format "SELECT contact.id as id,
                     cell_phone as address
              FROM contact
              JOIN notification_recipient ON
                   recipient_id = contact.id AND
                   recipient_type = 'C'      AND
                   notification = '%s' AND
                   last_status = 'PROCESSING'"
             (:id notification))]
    (into [] results)))

(defn group-rcpt
  "Expands a group with all non processed contacts yet"
  [group notification]
  (let [contacts (sql/with-query-results results
                   [(format "SELECT contact.id as id,
                                    cell_phone as address,
                                    contact_group_id as group_id
                             FROM contact
                             JOIN contact_group_member ON
                                  contact_id = contact.id AND
                                  contact_group_id = %s
                             JOIN notification_recipient ON
                                  recipient_id = contact_group_id AND
                                  recipient_type = 'G' AND
                                  last_status = 'PROCESSING' AND
                                  notification = '%s'
                             WHERE contact.id NOT IN
                                   (SELECT recipient_id
                                    FROM message_delivery
                                    WHERE notification = '%s' AND
                                          status IN ('CONNECTED', 'CANCELLED'))"
                      (:recipient_id group) (:id notification) (:id notification))]
                   (into [] results))]
    ;;TODO: is it possible for contacts to be empty in a non finished
    ;;group?
    ;;shouldn't I be finishing the group now?
    contacts)) 

(defn join-contacts
  "Joins repeated contacts belonging to different groups into a single entry.
   If a contact belongs to more than one group a new member :groups contains
   all the belonging groups

   {1 {:id 1 :address 0992123 :groups [1 2 3]}}"
  [contact-list]
  (reduce (fn [res contact]
            (if-let [groups (get-in res [(:id contact) :groups])]
              (assoc-in res [(:id contact) :groups] (conj groups (:group_id contact)))
              (assoc res (:id contact) (assoc contact :groups [(:group_id contact)]))))
          {} contact-list))

(defn expand-rcpt
  "Expands the recipient list groups, creating a set of contacts
   (i.e. if a contact belongs to more than one group, it appears
    only once).
   Each contact is returned with the original recipient id and type"
  [recipients notification]
  (sql/with-connection *database-url*
    (let [direct-contacts (contact-rcpt notification) ;;no expansion needed
          group-rcpts (filter #(= "G" (:recipient_type %)) recipients)
          group-contacts (apply concat (map #(group-rcpt % notification) group-rcpts))]
      (join-contacts (concat direct-contacts group-contacts)))))


(defn save-delivery
  "Saves the connection attempt"
  [contact notification result cause]
  (sql/with-connection *database-url*
    (sql/insert-values :message_delivery
                       [:notification :recipient_id :recipient_type :delivery_address :status]
                       [(:id notification) (:id contact) "C" (:address contact) result])))

(defn remaining-attempts?
  "Counts how many attempts for each type a contact has on a given
   notification"
  [contact notification]
  (sql/with-connection *database-url*
    (let [attempts (first (sql/with-query-results results
                            [(format "SELECT count(case when status='BUSY' then 1 else null end) as busy,
                                     count(case when status='NO ANSWER' then 1 else null end) as no_answer,
                                     count(case when status='FAILED' then 1 else null end) as failed,
                                     count(case when status='CONNECTED' then 1 else null end) as connected
                              FROM message_delivery
                              WHERE recipient_id = %s AND
                                    recipient_type = 'C' AND
                                    notification = '%s'"
                                     (:id contact) (:id notification))]
                            (into [] results)))]
      (or (nil? attempts)
          ;;TODO: check against proper notification thresholds
          (and (-> (:connected attempts) (> 0) not)
               (< (:failed attempts) 3)))))) 

(defn- update-group-results
  "Update the group recipient counters and status for a given notification.
   When no more contacts are left to be reached for this group it's flagged as finished"
  [group-id notification]
  (when group-id
    (sql/with-connection *database-url*
      (let [remaining (first (group-rcpt {:recipient_id group-id} notification))
            last_status (if remaining "PROCESSING" "FINISHED")]
        (sql/with-query-results results
          [(format "UPDATE notification_recipient
                    SET last_status = '%s',
                       connected = results.connected,
                 failed = results.failed,
                 attempts = results.total
             FROM (SELECT
                     count(case when status = 'CONNECTED' then 1 else null end) as connected,
                     count(case when status <> 'CONNECTED' then 1 else null end) as failed,
                     count(*) as total
                   FROM message_delivery d
                   WHERE d.notification = '%s' AND
                         d.recipient_id IN
                         (SELECT contact_id
                          FROM contact_group_member
                          WHERE contact_group_id=%s)) AS results
            WHERE notification_recipient.notification = '%s' AND
                  notification_recipient.recipient_id = '%s' AND
                  notification_recipient.recipient_type = 'G'
            RETURNING *"
                   last_status (:id notification) group-id (:id notification) group-id)])))))

(defn- update-contact-result
  "In case the contact was selected as a single entry
   in the notification update the last status of the recipient entry.
   This is done even if the contact also belongs to a group in order to 'close'
   the entry and let the notification process finish"
  [contact notification status]
  (sql/with-connection *database-url*
    (sql/update-values :notification_recipient
                       ["notification=? AND recipient_id=? AND recipient_type=?"
                        (:id notification) (:id contact) "C"]
                       {:last_status status})))

(defn save-result
  "Updates contact trial and returns the result"
  [notification contact result]
  (let [has_chance? (remaining-attempts? contact notification) 
        status (if (:error result)
                 (or (and has_chance? "FAILED") "CANCELLED")
                 "CONNECTED")]
    (save-delivery contact notification status (:cause result))
    (when (> (count (:groups contact)) 0)
      (doall (map #(update-group-results % notification) (:groups contact))))
    (update-contact-result contact notification status)
    ;;this value's gonna reach the main loop who decides whether to
    ;;continue dialing on this contact or not
    ;;TODO: DELAY should be solved according to status and retry
    {:status status :contact contact}))

(defn- call
  "Call a contact and wait till the call ends.
   Function returns the hangup event or nil if timedout"
  [context notification contact]
  (manager/with-connection context
    (let [call-id (.toString (java.util.UUID/randomUUID))
          prom (manager/set-user-data! call-id (promise))
          response (manager/action :Originate
                                   {:Channel (format "SIP/1/%s" (:address contact))
                                    :Context "test-context"
                                    :Exten "1000"
                                    :Priority "1"
                                    :Timeout 60000
                                    :CallerID "99970"
                                    :Variables [(format "MESSAGE=%s" (:message notification))
                                                (format "CALLID=%s" call-id)]})]
      (save-result notification
                   contact
                   (if (manager/success? response)
                     (deref prom 120000 {:error ::timeout}) ;;TODO
                     ;;where to i get timeout from? it depends on the
                     ;;call/prompt length
                     {:error ::congestion})))))

(defn- dispatch-calls
  "Returns the list of futures of each call thread (one p/contact)"
  [context notification contacts]
  (map #(future (call context notification %)) contacts))

(defn- get-available-ports
  "Returns the notification number of available ports to dial on"
  [notification]
  1) ;;TODO: do it!

(defn- process
  "Loops until all contacts for a notification are reached or finally
   cancelled"
  [notification context]
  (let [total-ports (get-available-ports notification)
        contact-list (-> (retrieve-rcpt notification) (expand-rcpt notification))]
    (loop [remaining contact-list pending-contacts []]
      (when remaining
        (let [pending (filter (comp not realized?) pending-contacts)
              finished (filter realized? pending-contacts)
              failed (filter (fn [r] (= (:status @r) "FAILED")) finished)
              free-ports (- total-ports (count pending))
              contacts (take free-ports remaining)
              dialing (dispatch-calls context notification contacts)]
          (Thread/sleep 100)
          (recur (concat (drop free-ports remaining) (map :contact failed))
                 (concat pending dialing)))))))

(defn dispatch
  "Motification dispatch function entry point, receives
   the notification to process"
  [notification]
  (manager/with-config {:name "192.168.0.127"}
    (if-let [context (manager/login "guille" "1234" :with-events)]
      (manager/with-connection context
        (process notification context))
      (println "Unable to login"))))
