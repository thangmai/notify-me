(ns notify-me.models.validation.trunk)

(def rules
  { :name  {:validations
            {:unique true
             :required true
              :max-length 50}
             :messages
            {:unique "El nombre %s ya esta en uso"
             :required "El nombre del troncal es requerido."
              :max-length "El largo del nombre no puede superar los 50 caracteres"}}
   })
