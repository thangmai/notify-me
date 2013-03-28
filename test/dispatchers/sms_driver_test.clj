(ns dispatchers.sms-driver-test
  (:require 
   [notify-me.models.contact :as contact]
   [notify-me.models.group :as group]
   [sms-drivers.driver :as driver]
   [clj-ancel-sms.messaging :as messaging]
   [clj-ancel-sms.administration :as admin]
   [slingshot.support :as s])
  (:use midje.sweet
        korma.core
        [slingshot.slingshot :only [try+ throw+]]))

;;creo un contacto valido que se registra bien y que queda bien en la base de datos
(fact "Create and remove a valid contact"
      (do 
        (let [c (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              saved (contact/one {:id (:id c)})
              _ (contact/delete! c)]
          (select-keys saved [:name :type :cell_phone])) => {:name "contact 1" :type "P" :cell_phone "1"}
        (provided 
         (admin/phone-registered? "1" "1") => false :times 1
         (admin/register-phone "1" "1" "1") => true :times 1
         (admin/unregister-phone "1" "1" "1") => true :times 1)))


;;creo un contacto que ya esta registrado valido que se crea bien y que no explota por el aire (y en teoria que loguea bien)
(against-background 
 [(around :contents (let [state (atom nil)] ?form))]
 (fact "Create and remove a valid contact"
       (do 
         (let [c1 (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
               saved (contact/one {:id (:id c1)})
               _ (contact/delete! c1)]
           (select-keys saved [:name :type :cell_phone])) => {:name "contact 1" :type "P" :cell_phone "1"} 
           (provided 
            (admin/phone-registered? "1" "1") => false :times 1
            (admin/register-phone "1" "1" "1") => true :times 1
            (admin/unregister-phone "1" "1" "1") => true :times 1)
         (let [c2 (contact/create! {:name "contact 2" :type "P" :cell_phone "1"})
               saved (contact/one {:id (:id c2)})
               _ (contact/delete! c2)]
           (select-keys saved [:name :type :cell_phone])) => {:name "contact 2" :type "P" :cell_phone "1"} 
           (provided 
            (admin/phone-registered? "1" "1") => true :times 1
            (admin/unregister-phone "1" "1" "1") => true :times 1))))


;;borro un contacto que no estaba registrado valido que se borra bien a pesar que haga throw+
(fact "Remove a non registered contact"
      (do 
        (let [c (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              _ (contact/delete! c)
              saved (contact/one {:id (:id c)})]
          saved) => nil
        (provided
         (admin/phone-registered? "1" "1") => false :times 1
         (admin/register-phone "1" "1" "1") => true :times 1
         (admin/unregister-phone "1" "1" "1") =throws=> (s/get-throwable 
                                                         (s/make-context {:type ::invalid} 
                                                                         "throw message" 
                                                                         (s/stack-trace) 
                                                                         (s/environment))) :times 1)))

;;updateo un contacto que no pertenece a ningun grupo le cambio el telefono (se tiene que desregistrar y registrar el nuevo)
(fact "Update a contact number, no groups"
      (do 
        (let [c (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              saved (contact/one {:id (:id c)})
              c (contact/update! c {:cell_phone "2"})
              _ (contact/delete! c)]
          (select-keys saved [:name :type :cell_phone])) => {:name "contact 1" :type "P" :cell_phone "1"}
        (provided 
         (admin/phone-registered? "1" "1") => false :times 1
         (admin/phone-registered? "1" "2") => false :times 1
         (admin/register-phone "1" "1" "1") => true :times 1
         (admin/register-phone "1" "1" "2") => true :times 1
         (admin/unregister-phone "1" "1" "1") => true :times 1
         (admin/unregister-phone "1" "1" "2") => true :times 1)))

;;creo grupo sin ningun contacto, valido que se registra bien
(fact "Create and remove a valid group without contacts"
      (do 
        (let [g (group/create! {:name "grupo 1" :members []})
              saved (group/one {:id (:id g)})
              _ (group/delete! g)]
          (select-keys saved [:name])) => {:name "grupo 1"}
        (provided 
         (admin/create-group "1" "grupo 1") => true :times 1
         (admin/delete-group "1" "grupo 1") => true :times 1
         (admin/group-exists? "1" "grupo 1") => false :times 1)))

;;creo grupo con un contacto, valido que se registra bien y se agrega al contacto
(fact "Create and remove a valid group"
      (do 
        (let [c (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              g (group/create! {:name "grupo 1" :members [c]})
              saved (group/one {:id (:id g)})
              _ (contact/delete! c)
              _ (group/delete! g)]
          (select-keys saved [:name])) => {:name "grupo 1"}
        (provided 
         (admin/phone-registered? "1" "1") => false :times 1
         (admin/register-phone "1" "1" "1") => true :times 1
         (admin/unregister-phone "1" "1" "1") => true :times 1
         (admin/create-group "1" "grupo 1") => true :times 1
         (admin/delete-group "1" "grupo 1") => true :times 1
         (admin/group-exists? "1" "grupo 1") => false :times 1
         (admin/add-phone-to-group "1" "1" "grupo 1") => true :times 1)))

;;modifico grupo de dos contacto, borro uno y agrego otro, valido que se llaman los add y remove to group correspondientes
(fact "Update a group removing and adding contacts"
      (do 
        (let [c1 (contact/create! {:name "contact 1" :type "P" :cell_phone "1"})
              c2 (contact/create! {:name "contact 1" :type "P" :cell_phone "2"})
              c3 (contact/create! {:name "contact 1" :type "P" :cell_phone "3"})
              g (group/create! {:name "grupo 1" :members [c1 c2]})
              saved (group/one {:id (:id g)})
              _ (group/update! {:id (:id g)} (assoc g :members [c2 c3]))
              _ (contact/delete! c1)
              _ (contact/delete! c2)
              _ (contact/delete! c3)
              _ (group/delete! g)]
          (select-keys saved [:name])) => {:name "grupo 1"}
        (provided 
         (admin/phone-registered? "1" anything) => false :times 3
         (admin/register-phone "1" "1" anything) => true :times 3
         (admin/unregister-phone "1" "1" anything) => true :times 3
         (admin/create-group "1" "grupo 1") => true :times 1
         (admin/delete-group "1" "grupo 1") => true :times 1
         (admin/group-exists? "1" "grupo 1") => false :times 1
         (admin/add-phone-to-group "1" "1" "grupo 1") => true :times 1
         (admin/add-phone-to-group "1" "2" "grupo 1") => true :times 1
         (admin/add-phone-to-group "1" "3" "grupo 1") => true :times 1
         (admin/rmv-phone-from-group "1" "1" "grupo 1") => true :times 1)))


;;updateo contacto perteneciente a 2 grupos, valido que se borra y agrrega en ambos



