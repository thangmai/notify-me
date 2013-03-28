(ns notify-me.models.validation.policy)

(def rules
  {:name  {:validations {:unique true
                          :required true
                          :max-length 50}
            :messages {:unique "El nombre %s ya esta en uso"
                       :required "El nombre de las politicas de entrega es requerido."
                       :max-length "El largo del nombre no puede superar los 100 caracteres"}}
  :retries_on_error {:validations {:required true
                                   :range [0 5]}
                     :messages {:required "Los reintentos por error son requeridos"
                                :range "Los reintentos deben ser un valor entre 0 y 5"}}
  :retries_on_busy {:validations {:required true
                                   :range [0 10]}
                     :messages {:required "Los reintentos por ocupado son requeridos"
                                :range "Los reintentos deben ser un valor entre 0 y 10"}}
  :no_answer_retries {:validations {:required true
                                    :range [0 10]}
                      :messages {:required "Los reintentos por no atencion son requeridos"
                                 :range "Los reintentos deben ser un valor entre 0 y 10"}}
  :no_answer_interval_secs {:validations {:required true
                                          :range [10 600]}
                            :messages {:required "Los segundos de reintento por no atencion son requeridos"
                                       :range "Los segundos de reintento deben ser un valor entre 10 y 600"}}
  :busy_interval_secs {:validations {:required true
                                     :range [10 600]}
                       :messages {:required "Los segundos de reintento por ocupado son requeridos"
                                  :range "Los segundos de reintento deben ser un valor entre 10 y 600"}}})
