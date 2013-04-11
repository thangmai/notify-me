(ns notify-me.models.validation.group)

(def rules
  { :name  {:validations {:required true
                          :unique true
                          :max-length 50}
             :messages {:required "El nombre del grupo es requerido"
                        :unique "El nombre %s ya esta en uso, seleccione otro por favor."
                        :max-length "El largo del nombre no puede superar los 50 caracteres"}}
   :description {:validations { :max-length 100}
                 :mesages {:required "La descripción no debe superar los 100 caracteres"}}})
