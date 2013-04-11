(ns notify-me.models.validation.notification)

(def rules
  {:type  {:validations { :required true }
           :messages { :required "Debe seleccionar algún mecanismo de despacho, sms, llamada, o ambos."}}
   :message  {:validations { :required true}
              :messages { :required "El mensaje a enviar es requerido, por favor ingréselo."}}
   :members {:validations { :required true}
             :messages{:required "Debe seleccionar algún destinatario de la notificación"}}})
