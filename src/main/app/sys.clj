(ns app.sys
  (:require
   [app.db :as db]
   [app.server :as server]
   [taoensso.timbre :as log]))

;; FIXME: Consider using syringe for dep management.
;; Until then, this ns provides a functional approach (without atoms and such) to deal with system state.

;; Creates a map of system state.
(defn start []
  (log/info "Starting system...")
  (let [sys {:db-node (db/start-cluster-node)}
        sys (assoc sys :server-stop-fn (server/start sys))]
    (log/info "System started:" (keys sys))
    sys))

;; Given a map of system state, stops the system.
(defn stop [{:keys [db-node server-stop-fn] :as sys}]
  (log/info "Stopping system...")
  (when db-node
    (try (.close db-node)
         (catch Throwable t
           (log/error t "Caught trying to close db-node."))))
  (when server-stop-fn
    (try (server-stop-fn)
         (catch Throwable t
           (log/error t "Caught trying to stop server.")))))
