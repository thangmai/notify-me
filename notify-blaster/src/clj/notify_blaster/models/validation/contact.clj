
(ns notify-blaster.models.validation.contact)

(def rules
  { :name  {:validations
            {:required true
              :max-length 100}
             :messages
             {:required "El nombre del contacto es requerido."
              :max-length "El largo del nombre no puede superar los 100 caracteres"}}
   :type {:validations {
                        :required true}
          :mesages {
                    :required "El tipo de contacto es requerido"}}
   :cell_phone  {:validations
                 {:required true
                  :unique true
                  :max-length 20}
                 :messages
                 {:required "El telefono es requerido."
                  :unique "El telefono %s ya esta en uso en otro contacto."
                  :max-length "El largo del telefono no puede superar los 20 caracteres"}}
   })
