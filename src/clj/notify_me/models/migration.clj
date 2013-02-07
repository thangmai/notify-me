(ns notify-me.models.migration
  (:require [clojure.java.jdbc :as sql]
            [notify-me.config :as config]))

(defn create-database
  []
  (sql/with-connection (:database-url config/opts)
    (sql/create-table :offices
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]
                      [:description :varchar])
    (sql/create-table :roles
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"])
    (sql/create-table :users
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]
                      [:password :varchar "NOT NULL"]
                      [:role :integer "REFERENCES roles(id)"]
                      [:office :integer "REFERENCES offices(id)"])
    (sql/create-table :contacts
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]
                      [:type :varchar "NOT NULL"]
                      [:cell_phone :varchar "NOT NULL"])
    (sql/create-table :contact_groups
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]
                      [:description :varchar]
                      [:type :varchar "NOT NULL"])
    (sql/create-table :contact_group_members
                      [:contact_group :integer "REFERENCES contact_groups(id)"]
                      [:contact_id :integer "REFERENCES contacts(id)"]
                      [:group_id :integer "REFERENCES contact_groups(id)"])
    (sql/create-table :delivery_policies
                      [:id :serial "PRIMARY KEY"]
                      [:name :varchar "NOT NULL"]
                      [:no_answer_retries :integer "NOT NULL" "DEFAULT 5"]
                      [:retries_on_error :integer "NOT NULL" "DEFAULT 10"]
                      [:busy_intervals_secs :integer "NOT NULL" "DEFAULT 60"]
                      [:no_answer_interval_secs :integer "NOT NULL" "DEFAULT 300"]
                      [:no_answer_disposition :varchar "NOT NULL" "DEFAULT 'CANCELED'"])
    (sql/create-table :notifications
                      [:id :varchar "PRIMARY KEY"]
                      [:status :varchar "NOT NULL"]
                      [:created :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
                      [:message :varchar "NOT NULL"]
                      [:delivery_policy :integer "REFERENCES delivery_policies(id)"])
    (sql/create-table :notification_recipients
                      [:notification :varchar "REFERENCES notifications(id)"]
                      [:recipient_id :integer "NOT NULL"]
                      [:recipient_type :varchar "NOT NULL"]
                      [:last_status :varchar "NOT NULL"])
    (sql/create-table :delivered_messages
                      [:notification :varchar "REFERENCES notifications(id)"]
                      [:recipient_id :integer "NOT NULL"]
                      [:recipient_type :varchar "NOT NULL"]
                      [:delivery_date :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
                      [:delivery_address :varchar "NOT NULL"]
                      [:status :varchar "NOT NULL"])
    )
  )

(defn -main
  []
  (print "Migrating database...") (flush)
  (create-database)
  (println " done"))
