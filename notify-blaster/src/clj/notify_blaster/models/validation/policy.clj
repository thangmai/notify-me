(ns notify-blaster.models.validation.policy)

(def rules
  { :name  {:validations
            {:unique true
             :required true
              :max-length 50}
             :messages
            {:unique "El nombre %s ya esta en uso"
             :required "El nombre de las politicas de entrega es requerido."
              :max-length "El largo del nombre no puede superar los 100 caracteres"}}
   })
