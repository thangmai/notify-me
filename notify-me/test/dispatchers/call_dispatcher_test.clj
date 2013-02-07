(ns dispatchers.call-dispatcher-test
  (:require [dispatchers.call :as d]
            [dispatchers.model :as model]
            [notify-me.models.notification :as notification])
  (:use midje.sweet
        dispatchers.data-access-test
        korma.core))

;;CALL PROCESSING BASIC FUNCTIONS
(against-background
 [(around :contents
          (let [n (set-notification
                   (notification/create! {:id "200006"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id (:id (get-policy 1))
                                          :trunk_id (:id (get-trunk 1))
                                          :members [{:id (:id (get-contact 1)) :type "C"}
                                                    {:id (:id (get-contact 2)) :type "C"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]

 (fact "Ports are retrieved correctly"
       (d/get-available-ports n) => 1)

 (fact "Notification status is updated correctly"
       (do
         (model/update-status! n "FINISHED")
         (find-notification n))    => (contains {:status "FINISHED"}))
 
 (fact "Call dispatching correctly joins"
       (let [pending (d/dispatch-calls nil n contacts)
             joined (map (fn [p] @p) pending)]
         joined) => [{:status "CONNECTED"} {:status "CONNECTED"}]
         (provided
          (d/call anything anything anything) => {:status "CONNECTED"} :times 2))

 (fact "Call processing correctly ends"
       (do
         (d/process n nil) => (contains {:status "FINISHED"}))
         (provided
          (d/call anything anything anything) => {:status "CONNECTED"} :times 2)
         (find-notification n) => (contains {:status "FINISHED"})))

(against-background
 [(around :contents
          (let [n (set-notification
                   (notification/create! {:id "200007"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id (:id (get-policy 1))
                                          :trunk_id (:id (get-trunk 1))
                                          :members [{:id (:id (get-contact 1)) :type "C"}
                                                    {:id (:id (get-contact 2)) :type "C"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Call processing correctly ends when the contacts are CANCELLED"
       (do
         (d/process n nil) => (contains {:status "FINISHED"}))
         (provided
          (d/call anything anything anything) => {:status "CANCELLED"} :times 2)
         (find-notification n) => (contains {:status "FINISHED"})))


(against-background
 [(around :contents
          (let [n (set-notification
                   (notification/create! {:id "200007"
                                          :type "CALL"
                                          :status "CREATED"
                                          :message "message"
                                          :delivery_policy_id (:id (get-policy 1))
                                          :trunk_id (:id (get-trunk 1))
                                          :members [{:id (:id (get-group 3)) :type "G"}]}))
                recipients (model/retrieve-rcpt n)
                contacts (model/expand-rcpt recipients n)]
            ?form))
  (before :contents (reset! data-atom (create-data)) :after (do (notification/delete! @notif)
                                                                (drop-data)))]
 (fact "Call processing correctly ends when the recipient is a group and all contacts connect"
       (do
         (d/process n nil) => (contains {:status "FINISHED"}))
         (provided
          (d/call anything anything anything) => {:status "CONNECTED"} :times 4)
         (find-notification n) => (contains {:status "FINISHED"})))


;;call delay between calls
;;call result is correctly parsed

;;error handling against asterisk is done gracefully

;;cancel a running notification
