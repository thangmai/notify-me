(ns notify-me.models.validation.contact)

(def rules
  { :name  {:validations {:required true
                          :max-length 100}
            :messages {:required "El nombre del contacto es requerido."
                        :max-length "El largo del nombre no puede superar los 100 caracteres"}}
   :type {:validations {:required true}
          :mesages {:required "El tipo de contacto es requerido"}}
   :cell_phone  {:validations {:required true
                               :unique true
                               :regex #"^(099|091|098)(\d{6})$"
                               :max-length 20}
                 :messages {:required "El teléfono es requerido."
                            :unique "El teléfono %s ya esta en uso en otro contacto."
                            :max-length "El largo del teléfono no puede superar los 20 caracteres"
                            :regex "Solamente teléfonos de ancel son permitidos, comenzando con 099, 098 y 091"}}})
