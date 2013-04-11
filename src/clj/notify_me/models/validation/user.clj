(ns notify-me.models.validation.user)

(def rules
  { :username  {:validations {:required true
                              :unique true
                              :max-length 50}
                :messages {:required "El nombre es requerido"
                           :unique "El nombre %s ya esta en uso, seleccione uno diferente."
                           :max-length "El nombre de la oficina debe contener menos de 50 caracteres"}}
   :email {:validations {:required true
                         :max-length 255
                         :email true}
           :messages {:required "El email es requerido"
                      :max-length "El largo del email debe ser menor a 255 caracteres"
                      :email "El campo email no contiene un email valido"}}
   :display_name {:validations {:max-length 255}}
   :password {:validations {:required true
                            :min-length 5
                            :max-length 255}
              :messages {:required "La clave es requerida"
                         :min-length "El largo mínimo de la clave son 5 caracteres"
                         :max-length "El largo máximo de la clave son 255 caracteres"}}
   :password-match {:validations {:required true
                                  :matches :password}
                    :messages {:required "Debe reingresar la clave"
                               :matches "Las claves no coinciden"
                               }}
   :roles {:validations {:required true}}})
