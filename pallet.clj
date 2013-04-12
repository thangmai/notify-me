;;; Pallet project configuration file

;;; By default, the pallet.api and pallet.crate namespaces are already referred.
;;; The pallet.crate.automated-admin-user/automated-admin-user us also referred.
;;; :phases {:configure (plan-fn
            ;;;           (println "do something")
         ;;;              (java/java :openjdk)
      ;;;                 (postgres)
       ;;;        (git))}

;;; (require '[your-ns :refer [your-group]])

(require '[pallet.actions :refer [package package-manager]])

;;(require '[pallet.crate.postgres :refer [postgres]])
(require '[pallet.crate.git :refer [git]])
(require '[pallet.crate.java :as java])
(require '[pallet.crate.postgres :as postgres])
(require '[pallet.crate.runit :as runit])
(require '[pallet.actions :as actions])
(require '[pallet.script.lib :refer [ls mkdir cp sed-file]])

(require '[pallet.crate.service
           :refer [supervisor-config supervisor-config-map] :as service])

(defn postgres-user 
  []
  (actions/group "postgres")
  (actions/user "postgres" 
                :action :create
                :shell false
                :group "postgres"
                :system true))

(defproject notify-me
 
  :phases {:configure (plan-fn
                       (package-manager :update)
                       (package "curl")
                       (actions/remote-file "/root/log4j.properties"
                                            :local-file "src/clj/log4j.properties"
                                            :owner "root"
                                            :group "root"
                                            :action :create
                                            :mode "0644"
                                            :force true)
                       (pallet.actions/exec-script (cp "/root/log4j.properties" "/root/file-destino"))
                       (pallet.actions/exec-script (sed-file "/root/file-destino" "s/INFO/WARN/" {}))
                       (pallet.actions/exec-script (ls "/root")))}

  :groups [(group-spec "notify-me"
                       :extends [(git {})
                                 (runit/server-spec {})
                                 (postgres/server-spec {:version "9.1"
                                                        :strategy :packages})
                                 (java/server-spec {:vendor :sun
                                                    :version [7]})]
                       :phases {:pre-req (plan-fn
                                          (package-manager :update)
                                          (postgres-user)
                                          (package "curl")
                                          (package "sudo")
                                          (package "sox")
                                          (package "libsox-fmt-mp3"))})
           
           (group-spec "asterisk" 
                       :extends [(git {})]
                       :phases {:test (plan-fn
                                       (pallet.actions/exec-script (cp "/root/file-destino" "/root/file-destino2"))
                                       (pallet.actions/exec-script (sed-file "/root/file-destino" "s/INFO/WARN/" {})))})]

)
