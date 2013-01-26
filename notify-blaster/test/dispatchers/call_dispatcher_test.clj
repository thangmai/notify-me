(ns dispatchers.call-dispatcher-test
  (:require [dispatchers.call :as d]
            [notify-blaster.models.notification :as notification]
            [notify-blaster.models.contact :as contact]
            [notify-blaster.models.group :as group]
            [notify-blaster.models.entities :as e])
  (:use midje.sweet
        korma.core))


(def data-atom (atom nil))

(defn- create-data
  []
  (let [contact1 (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
        contact2 (contact/create! {:name "contact 2" :type "P" :cell_phone "2"})
        contact3 (contact/create! {:name "contact 3" :type "P" :cell_phone "3"})
        contact4 (contact/create! {:name "contact 4" :type "P" :cell_phone "4"})
        group1 (group/create! {:name "grupo 1" :members [contact1 contact2]})
        group2 (group/create! {:name "grupo 2" :members [contact3 contact4]})
        group3 (group/create! {:name "grupo 3" :members [contact1 contact2 contact3 contact4]})]
    {:contacts [contact1 contact2 contact3 contact4] :groups [group1 group2 group3]}))

(defn- drop-data
  []
  (doall (map #(group/delete! %) (:groups @data-atom)))
  (doall (map #(contact/delete! %) (:contacts @data-atom))))

(defn- get-contact
  [idx]
  (get-in @data-atom [:contacts (dec idx)]))

(defn- get-group
  [idx]
  (get-in @data-atom [:groups (dec idx)]))

(def notif (atom nil))

(defn- set-notification
  [n]
  (reset! notif n)
  @notif)

(defn find-recipient
  [recipient type notification]
  (first (select e/notification_recipient
                 (where {:recipient_id (:id recipient)
                         :recipient_type type
                         :notification (:id notification)}))))

(defn find-delivery
  [contact notification]
  (first (select e/message_delivery
                 (where {:recipient_id (:id contact)
                         :recipient_type "C"
                         :notification (:id notification)}))))

(defn find-notification
  [notification]
  (first (select e/notification
                 (where {:id (:id notification)}))))

;
;;CONTACTS EXPANSION TESTS
(against-background
 [(around :contents
          (let [n (set-notification
                   (notification/create! {:id "100002"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id (get-contact 1)) :type "C"}
                                                    {:id (:id (get-contact 2)) :type "C"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Only contacts get expanded correctly"
       (reduce #(conj %1 (:address (%2 1))) #{} contacts) => (just [#"1" #"2"]))
 (fact "First contact connected, notification unchanged"
       (let [c (get-contact 1)]
         (d/save-result n (get contacts (:id c)) {})
         (find-recipient c "C" n) => (contains {:last_status "CONNECTED"
                                                :attempts 1
                                                :connected 1
                                                :failed 0})
         (find-delivery c n)      => (contains {:delivery_address "1"
                                                :status "CONNECTED"})
         (find-notification n)    => (contains {:status "CREATED"})))
 (fact "Second contact failed, notification unchanged"
       (let [c (get-contact 2)]
         (d/save-result n (get contacts (:id c)) {:error "not connected"})
         (find-recipient c "C" n)  => (contains {:last_status "FAILED"
                                                 :attempts 1
                                                 :failed 1
                                                 :connected 0})
         (find-delivery c n)       => (contains {:delivery_address "2"
                                                 :status "FAILED"})
         (find-notification n)     => (contains {:status "CREATED"}))))


(against-background
 [(around :contents
          (let [g (get-group 1)
                n (set-notification
                   (notification/create! {:id "000000"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "One group contacts get expanded correctly"
       (reduce #(conj %1 (:address (%2 1))) #{} contacts) => (just [#"1" #"2"]))
 (fact "First group contact connected, notification unchanged"
       (let [c (get-contact 1)]
         (d/save-result n (get contacts (:id c)) {})
         ;;recipient group status
         (find-recipient g "G" n) => (contains {:last_status "PROCESSING"
                                                :attempts 1
                                                :connected 1
                                                :failed 0})
         ;;contact delivery created
         (find-delivery c n)  => (contains {:delivery_address "1"
                                            :status "CONNECTED"})
         ;;notification was updated
         (find-notification n)    => (contains {:status "CREATED"})))
 (fact "Second group contact failed, notification unchanged"
       (let [c (get-contact 2)]
         (d/save-result n (get contacts (:id c)) {:error "not connected"})
         (find-recipient g "G" n) => (contains {:last_status "PROCESSING"
                                                :attempts 2
                                                :failed 1
                                                :connected 1})
         (find-delivery c n)      => (contains {:delivery_address "2"
                                                :status "FAILED"})
         (find-notification n)    => (contains {:status "CREATED"}))))


(against-background
 [(around :contents
          (let [g1 (get-group 1)
                g3 (get-group 3)
                n (set-notification
                   (notification/create! {:id "000002"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g1) :type "G"}
                                                    {:id (:id g3) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Intersecting groups contacts get expanded correctly, only once, non repeating"
       (reduce #(conj %1 (:address (%2 1))) [] contacts) => (just ["1" "2" "3" "4"] :in-any-order))
 (fact "First group contact connected, notification unchanged, both groups updated ok"
       (let [c (get-contact 1)]
         (d/save-result n (get contacts (:id c)) {})
         ;;recipient group 1 status
         (find-recipient g1 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 1
                                                 :connected 1
                                                 :failed 0})
         ;;recipient group 2 status
         (find-recipient g3 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 1
                                                 :connected 1
                                                 :failed 0})
         ;;contact delivery created
         (find-delivery c n)  => (contains {:delivery_address "1"
                                            :status "CONNECTED"})
         ;;notification was updated
         (find-notification n)    => (contains {:status "CREATED"})))
 (fact "Second group contact failed, notification unchanged, both groups updated ok"
       (let [c (get-contact 2)]
         (d/save-result n (get contacts (:id c)) {:error "not connected"})
         (find-recipient g1 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 2
                                                 :failed 1
                                                 :connected 1})
         (find-recipient g3 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 2
                                                 :failed 1
                                                 :connected 1}) 
         (find-delivery c n)      => (contains {:delivery_address "2"
                                                :status "FAILED"})
         (find-notification n)    => (contains {:status "CREATED"}))))


;; RETRIES TESTS

(against-background
 [(around :contents
          (let [g1 (get-group 1)
                g3 (get-group 3)
                n (set-notification
                   (notification/create! {:id "000003"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g1) :type "G"}
                                                    {:id (:id g3) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Contact has no remaining attempts because of 3 fails"
       (let [c (get-contact 1)]
         (d/save-result n (get contacts (:id c)) {:error "error 1"})
         (d/remaining-attempts? c n) => true
         (d/save-result n (get contacts (:id c)) {:error "error 2"})
         (d/remaining-attempts? c n) => true
         (d/save-result n (get contacts (:id c)) {:error "error 3"})
         (d/remaining-attempts? c n) => false
         ;;recipient group 1 status
         (find-recipient g1 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 3
                                                 :connected 0
                                                 :failed 3})
         ;;recipient group 2 status
         (find-recipient g3 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 3
                                                 :connected 0
                                                 :failed 3})))
 (fact "Contact has no remaining attempts because it has connected"
       (let [c (get-contact 1)]
         (d/save-result n (get contacts (:id c)) {})
         (d/remaining-attempts? c n) => false
         ;;recipient group 1 status
         (find-recipient g1 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 4
                                                 :connected 1
                                                 :failed 3})
         ;;recipient group 2 status
         (find-recipient g3 "G" n) => (contains {:last_status "PROCESSING"
                                                 :attempts 4
                                                 :connected 1
                                                 :failed 3}))))


(future-fact "Test retries on busy")
(future-fact "Test retries on no answer")

;;GROUP CLOSED BECAUSE ALL CONTACTS FINISHED

(against-background
 [(around :contents
          (let [g (get-group 1)
                n (set-notification
                   (notification/create! {:id "000006"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient finished because all group contacts has connected"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)]
         (d/save-result n (get contacts (:id c1)) {})
         (d/save-result n (get contacts (:id c2)) {})
         ;;recipient group status
         (find-recipient g "G" n) => (contains {:last_status "FINISHED"
                                                :attempts 2
                                                :connected 2
                                                :failed 0}))))

(against-background
 [(around :contents
          (let [g (get-group 1)
                n (set-notification
                   (notification/create! {:id "000006"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient finished because all group contacts have failed"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)]
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         ;;recipient group status
         (find-recipient g "G" n) => (contains {:last_status "FINISHED"
                                                :attempts 8
                                                :connected 0
                                                :failed 8}))))

;;after saving, expansion retrieves only missing ones
;; (simulates service restart)


(against-background
 [(around :contents
          (let [g (get-group 3)
                n (set-notification
                   (notification/create! {:id "000006"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id 1
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (d/retrieve-rcpt n)
                contacts (d/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient re-expansion retrieves only unfinished contacts"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)
             c3 (get-contact 3)
             c4 (get-contact 4)]
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c1)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c2)) {:error "failed"})
         (d/save-result n (get contacts (:id c3)) {})
         ;;contact 4 has only one fail so it's not finished yet,
         ;;should be retrieved on expansion
         (d/save-result n (get contacts (:id c4)) {:error "failed"})
         ;;recipient group status
         (find-recipient g "G" n) => (contains {:last_status "PROCESSING"
                                                :attempts 10
                                                :connected 1
                                                :failed 9})
         (let [contacts (d/expand-rcpt (d/retrieve-rcpt n) n)]
           (reduce #(conj %1 (:address (%2 1))) [] contacts)) => (just ["4"]))))

;;notification closed because all recipients finished

(future-fact "Notification closed because all recipient finished")

;;make calls, only corresponding calls are made for each contact (3
;;fails for example) 1 connect, no extra dials

;;call delay between calls
;;call result is correctly parsed

;;error handling against asterisk is done gracefully
