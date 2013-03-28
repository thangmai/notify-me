(ns notify-me.models.validation.core
  (:require [notify-me.utils :as utils]))

(def ^{:dynamic true} *is-unique?* (fn [value id] false))
(def ^{:dynamic true} *default-messages* {:unique "Value %s is aready taken, please choose another one"
                                          :required "Field is required"
                                          :min-length "Field is shorter than required"
                                          :max-length "Field is longer than required"
                                          :range "Field value %s is not in range"
                                          :email "Field must be a valid email address"
                                          :matches "Fields do not match"
                                          :regex "Field is not valid"
                                          :digits "Field must be a number"})
(defmulti valid-field? :type)

(defmethod valid-field? :default
  [field]
  true)

(def ^:dynamic *email-regex*
  #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defmethod valid-field? :email
  [{value :value}]
  (and value (re-matches *email-regex* value)))

(defmethod valid-field? :unique
  [{value :value form-values :form-values}]
  ;TODO: uniqueness function should be field-name based
  (*is-unique?* value (:id form-values)))

(defmethod valid-field? :digits
  [{value :value}]
  (and value (re-matches #"\d+" value)))

(defmethod valid-field? :range
  [{value :value r :rule}]
  (when (and (> (count value) 0) (re-matches #"\d+" value)) 
    (let [nval (utils/str->int value)]
      (and (>= nval (r 0))
           (<= nval (r 1))))))

(defmethod valid-field? :max-length
  [{value :value max :rule}]
  (<= (count value) max))

(defmethod valid-field? :matches
  [{value :value match :rule form-values :form-values}]
  (=  value (get form-values match)))

(defmethod valid-field? :regex
  [{value :value re :rule}]
  (and value (re-matches re value)))

(defmethod valid-field? :min-length
  [{value :value min :rule}]
  (>= (count value) min))

(defmethod valid-field? :required
  [{value :value f :rule}]
  (and (if (ifn? f) (f) f)
       (not (nil? value))
       (> (count value) 0)))

(defn get-message
  "Retrieves the corresponding error message for
   the failed rule, if not present use default"
  [name value rule-list failed-rule]
  (let [rule-id (failed-rule 0)
        message (or (get (:messages rule-list) rule-id)
                    (get *default-messages* rule-id))]
    (format message (str value))))

(defn append-on-error
  [errors name result]
  (if (not (= result true))
    (conj errors [name true])
    errors))

(defn join-deferreds
  "Wait for all deferreds to finish and then issue
   a callback with the complete list of errors"
  [pendings errors callback]
  (if-let [p (first pendings)]
    (let [rule-id (p 0)
          promise (-> (p 1) :deferred)
          afn (-> (p 1) :fn)]
      (.then promise
             (fn [response, status, jqxhr]
               (join-deferreds (rest pendings) (append-on-error errors rule-id (afn response)) callback))
             (fn [e]
               (join-deferreds (rest pendings) (append-on-error errors rule-id false) callback))))
    (callback errors)))


(defn validate-field-rules
  "Returns a list of error messages if the given field
   has any failed rule."
  [form-values field-rules callback]
  (let [name (field-rules 0)
        value (get form-values name)
        rule-list (field-rules 1)]
    (when rule-list
      (let [validations (doall (reduce #(merge %1 {(%2 0) (valid-field? {:type (%2 0)
                                                                         :value value
                                                                         :rule (%2 1)
                                                                         :form-values form-values})}) {} (:validations rule-list)))
            pendings (doall (filter #(and (coll? (% 1)) (-> (% 1) :deferred)) validations))
            errors (doall (filter #(not (% 1)) validations))]
        (join-deferreds pendings errors
                        (fn [errors]
                          (if (first errors)
                            (callback {name (map #(get-message name value rule-list %) errors)})
                            (callback nil))))))))

(defn validate
  "Validates a list of attributes of the form
   Given a set of validation rules"
  [form-values rules errors callback]
  (validate-field-rules
     form-values
     (first rules)
     (fn [failed-rules]
       (let [pending-rules (rest rules)
             acc-errors (merge failed-rules errors)]
         (if (empty? pending-rules)
           (callback acc-errors)
           (validate form-values pending-rules acc-errors callback))))))


