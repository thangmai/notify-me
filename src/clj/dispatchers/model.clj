(ns dispatchers.model
  (:require [clojure.java.jdbc :as sql]
            [notify-me.models.trunk :as trunk]
            [notify-me.models.policy :as delivery-policies]
            [notify-me.models.notification :as notification-model]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def ^:dynamic *database-url* "postgresql://localhost/notify-me?user=guille&password=bogan731")

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

(defn direct-contacts
  "Return notification direct contacts with addresses expanded"
  [notification]
  (sql/with-connection *database-url*
    (contact-rcpt notification)))

(defn expand-rcpt
  "Expands the recipient list groups, creating a set of contacts
   (i.e. if a contact belongs to more than one group, it appears
    only once).
   Each contact is returned with the original recipient id and type"
  [recipients notification]
  (sql/with-connection *database-url*
    (let [direct-contacts (contact-rcpt notification) ;;no expansion needed
          group-rcpts (filter #(= "G" (:recipient_type %)) recipients)
          group-contacts (apply concat (map #(group-rcpt % notification) group-rcpts))
          joined-contacts (join-contacts (concat direct-contacts group-contacts))]
      (map #(get % 1) joined-contacts))))

(defn save-delivery
  "Saves the connection attempt"
  ([contact notification result cause]
     (save-delivery contact notification result cause "C"))
  ([recipient notification result cause recipient_type]
     (log/debug "Saving delivery recipient:" recipient " result:" result " cause:" cause)
     (sql/with-connection *database-url*
       (sql/insert-values :message_delivery
                          [:notification :recipient_id :recipient_type :delivery_address :status :cause]
                          [(:id notification) (:id recipient) recipient_type (:address recipient) result cause]))))

(defn remaining-attempts?
  "Counts how many attempts for each type a contact has on a given
   notification"
  [contact notification policies]
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
          (and (-> (:connected attempts) (> 0) not)
               (< (:failed attempts) (:retries_on_error policies))
               (< (:busy attempts) (:retries_on_busy policies))
               (< (:no_answer attempts) (:no_answer_retries policies))))))) 

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

;;TODO update counters!
(defn update-contact-result
  "In case the contact was selected as a single entry
   in the notification update the last status of the recipient entry.
   This is done even if the contact also belongs to a group in order to 'close'
   the entry and let the notification process finish"
  ([contact notification status]
     (update-contact-result notification contact "C" status))
  ([notification recipient recipient_type status]
     (sql/with-connection *database-url*
       (sql/update-values :notification_recipient
                          ["notification=? AND recipient_id=? AND recipient_type=?"
                           (:id notification) (:id recipient) recipient_type]
                          {:last_status status}))))

(defn get-policies
  [notification]
  (delivery-policies/one {:id (:delivery_policy_id notification)}))

(defmulti normal-clearing? (fn [notification _] (:type notification)))
(defmulti get-cause-name (fn [notification _] (:type notification)))

(defmethod normal-clearing? :default
  [notification result]
  (throw+ {:type ::missing-method 
           :description (format "Notification %s has no defined handler for normal-clearing?"
                                (:type notification))}))

(defmethod get-cause-name :default
  [notification result]
  (throw+ {:type ::missing-method
           :description (format "Notification %s has no defined handler for get-cause-name"
                                (:type notification))}))

(defn save-result
  "Updates contact trial and returns the result"
  [notification contact result]
  (let [policies (get-policies notification)
        has_chance? (remaining-attempts? contact notification policies) 
        status (if (not (normal-clearing? notification result)) 
                 (or (and has_chance? (get-cause-name notification result)) "CANCELLED")
                 "CONNECTED")]
    (save-delivery contact notification status (:Cause result))
    (when (> (count (:groups contact)) 0)
      (doall (map #(update-group-results % notification) (:groups contact))))
    (update-contact-result contact notification status)
    ;;this value's gonna reach the main loop who decides whether to
    ;;continue dialing on this contact or not
    (case status
      "BUSY" (Thread/sleep (* (:busy_interval_secs policies) 1000))
      "NO ANSWER" (Thread/sleep (* (:no_answer_interval_secs policies) 1000))
      :nop)
    {:status status :contact contact}))

(defn get-trunk
  [notification]
  (trunk/one {:id (:trunk_id notification)}))

(defn cancelling?
  [notification]
  (let [notification (notification-model/one {:id (:id notification)})]
    (= (:status notification) "CANCELLING")))

(defn update-status!
  [notification status]
  (notification-model/update! {:id (:id notification)}
                              {:status status}))
