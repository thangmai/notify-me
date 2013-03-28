(ns notify-me.models.validation.trunk)

(def rules
  {:name {:validations {:unique true
                        :required true
                        :max-length 50}
          :messages {:unique "El nombre %s ya esta en uso"
                     :required "El nombre del troncal es requerido."
                     :max-length "El largo del nombre no puede superar los 50 caracteres"}}
   :technology {:validations {:required true
                              :max-length 10}
                :messages {:required "La tecnologia es requerida"
                           :max-length "El largo maximo permitido es para la tecnologia 10"}}
   :number {:validations {:required true
                          :max-length 20}
            :messages {:required "El numero del troncal es requerido"
                       :max-length "El largo maximo permitido para el numero es 20"}}
   :context {:validations {:required true
                           :max-length 50}
             :messages {:required "El nombre del contexto de salida es requerido"
                        :max-length "El largo maximo permitido para el contexto es 50"}}
   :extension {:validations {:required true
                             :max-length 20}
               :messages {:required "La extension es requerida"
                          :max-length "El largo maximo permitido para la extension es 10"}}
   :priority {:validations {:required true
                            :max-length 20}
              :messages {:required "La prioridad es requerida"
                         :max-length "El largo maximo permitido para la prioridad es 20"}}
   :callerid {:validations {:required true
                            :max-length 20}
              :messages {:required "El caller id es requerido"
                         :max-length "El largo maximo permitido para el caller id es 20"}}
   :capacity {:validations {:required true
                            :digits true
                            :range [1 200]}
              :messages {:required "La capacidad es requerida"
                         :digits "La capacidad debe ser un numero"
                         :range "La capacidad del troncal debe ser un valor entre 1 y 200"}}
   :host {:validations {:required true
                        :max-length 100}
          :messages {:required "El host es requerido"
                     :max-length "El largo maximo permitido para el host id es 100"}}
   :user {:validations {:required true
                        :max-length 50}
          :messages {:required "El usuario es requerido"
                     :max-length "El largo maximo permitido para el usuario id es 50"}}
   :password {:validations {:required true
                            :max-length 50}
              :messages {:required "El password es requerido"
                         :max-length "El largo maximo permitido para el password id es 50"}}})
