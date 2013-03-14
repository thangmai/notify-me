(ns notify-me.sms-drivers.driver)

(defprotocol SmsDriver
  (group-dispatching? [this] "Returns true if the driver supports group dispatching")
  (sms-to-number [this number message] "Delivers a message to a single recipient")
  (sms-to-group [this group message]) "Delivers a message to a named group of recipients")

(defn- load-driver
  "Dynamically loads the driver needed"
  [namespace]
  (do
    (require (quote namespace))
    (symbol namespace "driver")))

(defn driver
  "Returns the driver to use for sms dispatching"
  []
  ;;TODO: this should be configured dynamically somewhere
  (load-driver "notify-me.sms-drivers.ancel"))