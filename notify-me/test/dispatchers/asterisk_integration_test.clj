(ns dispatchers.asterisk-integration-test
  (:require [dispatchers.call :as d]
            [dispatchers.model :as model]
            [clj-asterisk.manager :as manager])
  (:use midje.sweet
        dispatchers.data-access-test
        korma.core))

(against-background
 [(around :contents
          (manager/with-config {:name "192.168.0.127"}
            (if-let [context (manager/login "guille" "1234" :with-events)]
              (manager/with-connection context
                (let [contact {:address "55598123"}
                      notification {:message "this is a dispatch message"}]
                  ?form)))))]
 (fact "Call flow"
       (do
         (d/call context notification contact) => {:contact "1" :status "CONNECTED"})
         (provided
          (model/save-result anything anything anything) => {:contact "1" :status "CONNECTED"}
          (model/get-trunk anything) => {:technology "SIP"
                                         :number "1000"
                                         :context "test-context"
                                         :priority "1"
                                         :extension "1000"
                                         :callerid "99970"})))
