(ns dispatchers.sms-notification-test
  (:require [dispatchers.sms :as s]
            [dispatchers.model :as model]
            [notify-me.models.contact :as contact]
            [notify-me.models.group :as group]
            [sms-drivers.driver :as driver]
            [clj-ancel-sms.messaging :as messaging]
            [clj-ancel-sms.administration :as admin]
            [notify-me.models.notification :as notification]
            [notify-me.models.policy :as policy]
            [notify-me.jobs.dispatcher :as dispatcher]
            [slingshot.support :as sling-s])
  (:use midje.sweet
        dispatchers.data-access-test
        korma.core
        [slingshot.slingshot :only [try+ throw+]]))


(fact "Group recipient finished because dispatch has failed"
      (do 
        (let [c1 (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              c2 (contact/create! {:name "contact 1" :type "P" :cell_phone "2"})
              g (merge (group/create! {:name "grupo 1" :members [c1 c2]}) {:type "G"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004021"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id g) :type "G"}]}))
              recipients (model/retrieve-rcpt n)
              contacts (model/expand-rcpt recipients n)]
          (s/save-result n g {:error "failed"})
          (s/save-result n g {:error "failed"})
          (s/save-result n g {:error "failed"})
          (s/save-result n g {:error "failed"})
          ;;recipient group status
          (let [found-rcpt (find-recipient g "G" n)
                _ (contact/delete! c1)
                _ (contact/delete! c2)
                _ (group/delete! g)
                _ (notification/delete! n)
                _ (policy/delete! policy1)]
            found-rcpt)) => (contains {:last_status "CANCELLED"
                                      :attempts 8
                                      :connected 0
                                      :failed 8})
        (provided 
         (admin/phone-registered? "1" anything) => false :times 2
         (admin/register-phone "1" "1" anything) => true :times 2
         (admin/unregister-phone "1" "1" anything) => true :times 2
         (admin/create-group "1" "grupo 1") => true :times 1
         (admin/delete-group "1" "grupo 1") => true :times 1
         (admin/group-exists? "1" "grupo 1") => false :times 1
         (admin/add-phone-to-group "1" anything "grupo 1") => true :times 2)))


(fact "Contacts connected and cancelled"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004022"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id c1) :type "C"}
                                                  {:id (:id c2) :type "C"}]}))
              recipients (model/retrieve-rcpt n)
              contacts (model/expand-rcpt recipients n)]
          (s/save-result n c1 {:error "failed"})
          (s/save-result n c1 {:error "failed"})
          (s/save-result n c1 {:error "failed"})
          (s/save-result n c1 {:error "failed"})
          (s/save-result n c2 {:error "failed"})
          (s/save-result n c2 {:error "failed"})
          (s/save-result n c2 {:error "failed"})
          (s/save-result n c2 {})
          ;;recipient group status
          (let [r1 (find-recipient c1 "C" n)
                r2 (find-recipient c2 "C" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (notification/delete! n)
            (policy/delete! policy1)
            {:r1 (:last_status r1) :r2 (:last_status r2)})) => {:r1 "CANCELLED" :r2 "CONNECTED"}
            (provided 
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2)))


(fact "Real notification process, two contacts connect"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id c1) :type "C"}
                                                  {:id (:id c2) :type "C"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                r1 (find-recipient c1 "C" n)
                r2 (find-recipient c2 "C" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :r1 (:last_status r1) 
             :r2 (:last_status r2)})) => {:r1 "CONNECTED" :r2 "CONNECTED" :state "FINISHED"}
            (provided 
             (messaging/to-cellphone "1" anything "message") => true :times 2
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2)))


(fact "Real notification process, one contacts connect, one fails"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id c1) :type "C"}
                                                  {:id (:id c2) :type "C"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                r1 (find-recipient c1 "C" n)
                r2 (find-recipient c2 "C" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :r1 (:last_status r1) 
             :r2 (:last_status r2)})) => {:r1 "CONNECTED" :r2 "CANCELLED" :state "FINISHED"}
            (provided 
             (messaging/to-cellphone "1" "1" "message") => true :times 1
             (messaging/to-cellphone "1" "2" "message") => false :times 4
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2)))

(fact "Real notification process, one group connects"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              g (merge (group/create! {:name "grupo 1" :members [c1 c2]}) {:type "G"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id g) :type "G"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                g1 (find-recipient g "G" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (group/delete! g)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :g1 (:last_status g1)})) => {:g1 "CONNECTED" :state "FINISHED"}
            (provided 
             (messaging/to-group "1" "grupo 1" "message") => true :times 1
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2
             (admin/create-group "1" "grupo 1") => true :times 1
             (admin/delete-group "1" "grupo 1") => true :times 1
             (admin/group-exists? "1" "grupo 1") => false :times 1
             (admin/add-phone-to-group "1" anything "grupo 1") => true :times 2)))



(fact "Real notification process, two contacts fail to dispatch"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id c1) :type "C"}
                                                  {:id (:id c2) :type "C"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                r1 (find-recipient c1 "C" n)
                r2 (find-recipient c2 "C" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :r1 (:last_status r1) 
             :r2 (:last_status r2)})) => {:r1 "CANCELLED" :r2 "CANCELLED" :state "FINISHED"}
            (provided 
             (messaging/to-cellphone "1" anything "message") => false :times 8
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2)))



(fact "Real notification process, one group fails to dispatch"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              g (merge (group/create! {:name "grupo 1" :members [c1 c2]}) {:type "G"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id g) :type "G"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                g1 (find-recipient g "G" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (group/delete! g)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :g1 (:last_status g1)})) => {:g1 "CANCELLED" :state "FINISHED"}
            (provided 
             (messaging/to-group "1" "grupo 1" "message") => false :times 4
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2
             (admin/create-group "1" "grupo 1") => true :times 1
             (admin/delete-group "1" "grupo 1") => true :times 1
             (admin/group-exists? "1" "grupo 1") => false :times 1
             (admin/add-phone-to-group "1" anything "grupo 1") => true :times 2)))



(fact "Real notification process, one group, sending throws up"
      (do 
        (let [c1 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "1"}) {:type "C"})
              c2 (merge (contact/create! {:name "contact 1" :type "P" :cell_phone "2"}) {:type "C"})
              g (merge (group/create! {:name "grupo 1" :members [c1 c2]}) {:type "G"})
              policy1 (policy/create! {:name "test"})
              n (set-notification
                 (notification/create! {:id "00004023"
                                        :type "SMS"
                                        :status "CREATED"
                                        :message "message"
                                        :delivery_policy_id (:id policy1)
                                        :members [{:id (:id g) :type "G"}]}))]
          ;;recipient group status
          (let [result (dispatcher/dispatch n)
                g1 (find-recipient g "G" n)]
            (contact/delete! c1)
            (contact/delete! c2)
            (group/delete! g)
            (notification/delete! n)
            (policy/delete! policy1)
            {:state (:status result)
             :g1 (:last_status g1)})) => {:g1 "CANCELLED" :state "FINISHED"}
            (provided 
             (messaging/to-group "1" "grupo 1" "message")  =throws=> (sling-s/get-throwable 
                                                                      (sling-s/make-context {:type ::invalid} 
                                                                                      "throw message" 
                                                                                      (sling-s/stack-trace) 
                                                                                      (sling-s/environment))) :times 4
             (admin/phone-registered? "1" anything) => false :times 2
             (admin/register-phone "1" "1" anything) => true :times 2
             (admin/unregister-phone "1" "1" anything) => true :times 2
             (admin/create-group "1" "grupo 1") => true :times 1
             (admin/delete-group "1" "grupo 1") => true :times 1
             (admin/group-exists? "1" "grupo 1") => false :times 1
             (admin/add-phone-to-group "1" anything "grupo 1") => true :times 2)))