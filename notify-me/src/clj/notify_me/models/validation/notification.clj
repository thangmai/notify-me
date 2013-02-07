(ns notify-me.models.validation.notification)

(def rules
  {:type  {
           :validations
           {
            :required true
            }
           :messages
           {
            :required "Debe seleccionar algun mecanismo de despacho, sms, llamada, o ambos."}
           }
   :message  {
              :validations
              {
               :required true
               }
              :messages
              {
               :required "El mensaje a enviar es requerido, por favor ingreselo."}
              }
   :members {
             :validations
             {
               :required true
               }
             :messages
             {
               :required "Debe seleccionar algun destinatario de la notificacion"}
             }
   }
  )
