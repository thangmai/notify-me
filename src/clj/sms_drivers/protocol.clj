(ns sms-drivers.protocol)

(defprotocol SMSDriver
  (group-dispatching? [this] 
    "Returns true if the driver supports group dispatching")
  (sms-to-number [this number message] 
    "Delivers a message to a single recipient")
  (sms-to-group [this group message] 
    "Delivers a message to a named group of recipients"))
