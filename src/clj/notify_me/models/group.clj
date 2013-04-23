(ns notify-me.models.group
  (:require notify-me.models.db
            [notify-me.models.entities :as e])
  (:use korma.core
        korma.db
        notify-me.utils))


(defn create!
  [attributes]
  (let [group (select-keys attributes [:name :description :office_id :type])
        members (:members attributes)]
      (transaction
       (let [g (insert e/contact_group (values group))]
         (doall
          (map #(insert e/contact_group_member
                        (values {:contact_group_id (:id g)
                                 :contact_id (str->int (:id %))
                                 :office_id (:office_id g)})) 
               members))
         (assoc g :members members)))))

(defn update!
  [conditions attributes]
  (let [group (select-keys attributes [:name :description :type])
        members (:members attributes)]
    (transaction
     (update e/contact_group
            (set-fields group)
            (where conditions))
     ;;this is evil but there are no FK to the members so its easier
     ;;to prune and reinsert than to diff against the DB
     (delete e/contact_group_member
             (where {:contact_group_id (str->int (:id attributes))}))
     (doall (map #(insert e/contact_group_member
                          (values {:contact_group_id (str->int (:id attributes))
                                   :contact_id (str->int (:id %))
                                   :office_id (:office_id attributes)}))
                 members)))
    (assoc group :members members)))

(defn- get-members
  [group]
  (select e/contact
          (join :inner e/contact_group_member
                (and
                 (= :contact_group_member.contact_id :contact.id)
                 (= :contact_group_member.contact_group_id (:id group))))))

(defn search
  [conditions]
  (select e/contact_group (where conditions)))

(defn one
  [conditions]
  (when-let [group (first (select e/contact_group
                 (where conditions)
                 (limit 1)))]
    (assoc group :members (get-members group))))

(defn delete!
  [group]
  (transaction
   (delete e/contact_group_member
           (where {:contact_group_id (:id group)
                   :office_id (:office_id group)}))
   (delete e/contact_group
          (where {:id (:id group)}))))

(defn groups-for-contact
  "Retrieves all the groups belonging to a contact"
  [contact-id]
  (select e/contact_group
          (join :inner e/contact_group_member
                (and (= :contact_group_member.contact_id contact-id)))))

(defn all
  []
  (select e/contact_group))
