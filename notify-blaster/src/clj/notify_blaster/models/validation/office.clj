(ns notify-blaster.models.validation.office)

(def rules
  { :name  {:validations
             {:required true
              :unique true
              :max-length 50}
             :messages
             {:required "El nombre de la oficina es requerido"
              :unique "El nombre %s ya esta en uso, por favor seleccione uno diferente."
              :max-length "El largo del nombre no puede superar los 40 caracteres"}}
    :description {:validations
                   {:max-length 255}}})
