(ns notify-blaster.models.validation.office)

(def rules
  { "name"  {:validations
             {:required true
              :unique true
              :max-length 50}
             :messages
             {:required "Office name is required"
              :unique "Office name %s is already in use, please choose another one"
              :max-length "Office name must be shorter than 5 characters"}}
    "description" {:validations
                   {:max-length 255}}})
