(ns lobos.migrations
  (:refer-clojure :exclude [alter drop
                            bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema
               config helpers)))

(defmigration add-offices-table
  (up []
      (create
       (tbl :office
            (varchar :name 50 :unique)
            (varchar :description 255))))
  (down []
        (drop (table :office))))

(defmigration add-users-table
  (up []
      (create
       (tbl :user
            (varchar :username 50)
            (check :username (> (length :username) 1))
            (varchar :display_name 255)
            (varchar :email 255 :unique)
            (check :email (> (length :email) 1))
            (varchar :password 255)
            (text :roles)
            (refer-to :office)))
      (create (index :user [:username]))
      (create (index :user [:email]))
      (create (index :user [:username :office_id] :unique)))
  (down [] (drop (table :user))))

(defmigration add-contacts-table
  (up []
      (create
       (tbl :contact
            (varchar :name 100)
            (varchar :type 20)
            (varchar :cell_phone 20)
            (refer-to :office)))
      (create (index :contact [:cell_phone :office_id] :unique)))
  (down []
        (drop (table :contact))))

(defmigration add-contacts_groups_table
  (up []
      (create
       (tbl :contact_group
            (varchar :name 50)
            (varchar :description 100)
            (varchar :type 20)
            (refer-to :office)))
      (create (index :contact_group [:name :office_id] :unique)))
  (down []
        (drop (table :contact_group))))

(defmigration add-contact_group_members-table
  (up []
      
      (create
       (tbl :contact_group_member
            (refer-to :contact_group)
            (refer-to :contact)
            (refer-to :contact_group :group_id)
            (refer-to :office))))
  (down []
        (drop (table :contact_group_member))))

(defmigration add-delivery_policies-table
  (up []
      (create
       (tbl :delivery_policy
            (varchar :name 50 :unique)
            (integer :retries_on_error (default 3))
            (integer :busy_interval_secs (default 60))
            (integer :retries_on_busy (default 5))
            (integer :no_answer_interval_secs (default 300))
            (integer :no_answer_retries (default 5))
            (refer-to :office)))
      (create (index :delivery_policy [:name :office_id] :unique)))
  (down []
        (drop (table :delivery_policy))))

(defmigration add-trunks-table
  (up []
      (create
       (tbl :trunk
            (varchar :name 50)
            (varchar :technology 10)
            (varchar :number 20)
            (varchar :context 50)
            (varchar :extension 20)
            (varchar :priority 20)
            (varchar :callerid 20)
            (varchar :host 100)
            (varchar :user 50)
            (varchar :password 50)
            (integer :capacity)
            (refer-to :office))))
  (down []
        (drop (table :trunk))))

(defmigration add-sms_providers-table
  (up []
      (create
       (tbl :sms_provider
            (varchar :name 50)
            (varchar :provider 50))))
  (down []
        (drop (table :sms_provider))))

(defmigration add-notifications-table
  (up []
      (create
       (table :notification
              (varchar :id 50 :primary-key)
              (varchar :type 10)
              (varchar :status 20)
              (timestamp :created (default (now)))
              (varchar :message 200)
              (refer-to :office)
              (refer-to :sms_provider)
              (refer-to :trunk)
              (refer-to :delivery_policy))))
  (down []
        (drop (table :notification))))

(defmigration add-notification-recipients-table
  (up []
      (create
       (table :notification_recipient
              (varchar :notification 50 [:refer :notification :id])
              (integer :recipient_id)
              (varchar :recipient_type 20)
              (varchar :last_status 20)
              (integer :attempts)
              (integer :failed)
              (integer :connected))))
  (down []
        (drop (table :notification_recipient))))

(defmigration add-message_delivery-table
  (up []
      (create
       (table :message_delivery
              (varchar :notification 50 [:refer :notification :id])
              (integer :recipient_id)
              (varchar :recipient_type 20)
              (timestamp :delivery_date (default (now)))
              (varchar :delivery_address 20)
              (varchar :status 20)
              (varchar :cause 100))))
  (down []
        (drop (table :message_delivery))))

(defmigration add-sessions-table
  (up [] (create
          (tbl :user_session
               (varchar :key 255 :unique)
               (text :data)))
      (create (index :user_session [:key])))
  (down [] (drop (table :user_session))))

