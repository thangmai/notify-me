(ns dispatchers.call-database-test
  (:require [dispatchers.call :as d]
            [dispatchers.model :as model]
            [notify-me.models.notification :as notification])
  (:use midje.sweet
        dispatchers.data-access-test
        korma.core))


;;CONTACTS EXPANSION TESTS
(against-background
 [(around :contents
          (let [n (set-notification
                   (notification/create! {:id "100002"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id (get-contact 1)) :type "C"}
                                                    {:id (:id (get-contact 2)) :type "C"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Only contacts get expanded correctly"
       (reduce #(conj %1 (:address %2)) #{} contacts) => (just [#"1" #"2"]))
 (fact "First contact connected, notification unchanged"
       (let [c (get-contact 1)]
         (println "Get contact returned " (pr-str c))
         (model/save-result n (contact-by-id (:id c) contacts) {:Cause "16"})
         (find-recipient c "C" n) => (contains {:last_status "CONNECTED"
                                                :attempts 1
                                                :connected 1
                                                :failed 0})
         (find-delivery c n)      => (contains {:delivery_address "1"
                                                :status "CONNECTED"})
         (find-notification n)    => (contains {:status "CREATED"})))
 (fact "Second contact failed, notification unchanged"
       (let [c (get-contact 2)]
         (model/save-result n (contact-by-id (:id c) contacts) {:error "not connected"})
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "One group contacts get expanded correctly"
       (reduce #(conj %1 (:address %2)) #{} contacts) => (just [#"1" #"2"]))
 (fact "First group contact connected, notification unchanged"
       (let [c (get-contact 1)]
         (model/save-result n (contact-by-id (:id c) contacts) {:Cause "16"})
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
         (model/save-result n (contact-by-id (:id c) contacts) {:error "not connected"})
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g1) :type "G"}
                                                    {:id (:id g3) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Intersecting groups contacts get expanded correctly, only once, non repeating"
       (reduce #(conj %1 (:address %2)) [] contacts) => (just ["1" "2" "3" "4"] :in-any-order))
 (fact "First group contact connected, notification unchanged, both groups updated ok"
       (let [c (get-contact 1)]
         (model/save-result n (contact-by-id (:id c) contacts) {:Cause "16"})
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
         (model/save-result n (contact-by-id (:id c) contacts) {:error "not connected"})
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g1) :type "G"}
                                                    {:id (:id g3) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Contact has no remaining attempts because of 3 fails"
       (let [c (get-contact 1)]
         (model/save-result n (contact-by-id (:id c) contacts) {:error "error 1"})
         (model/remaining-attempts? c n (get-policy 1)) => true
         (model/save-result n (contact-by-id (:id c) contacts) {:error "error 2"})
         (model/remaining-attempts? c n (get-policy 1)) => true
         (model/save-result n (contact-by-id (:id c) contacts) {:error "error 3"})
         (model/remaining-attempts? c n (get-policy 1)) => false
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
         (model/save-result n (contact-by-id (:id c) contacts) {:Cause "16"})
         (model/remaining-attempts? c n (get-policy 1)) => false
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient finished because all group contacts has connected"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)]
         (model/save-result n (contact-by-id (:id c1) contacts) {:Cause "16"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:Cause "16"})
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient finished because all group contacts have failed"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)]
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
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
                                          :delivery_policy_id (:id (get-policy 1))
                                          :members [{:id (:id g) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Group recipient re-expansion retrieves only unfinished contacts"
       (let [c1 (get-contact 1)
             c2 (get-contact 2)
             c3 (get-contact 3)
             c4 (get-contact 4)]
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c1) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c2) contacts) {:error "failed"})
         (model/save-result n (contact-by-id (:id c3) contacts) {:Cause "16"})
         ;;contact 4 has only one fail so it's not finished yet,
         ;;should be retrieved on expansion
         (model/save-result n (contact-by-id (:id c4) contacts) {:error "failed"})
         ;;recipient group status
         (find-recipient g "G" n) => (contains {:last_status "PROCESSING"
                                                :attempts 10
                                                :connected 1
                                                :failed 9})
         (let [contacts (model/expand-rcpt (model/retrieve-rcpt n) n)]
           (reduce #(conj %1 (:address %2)) [] contacts)) => (just ["4"]))))

