(ns app.server
  (:require
   [app.parser :as parser]
   [org.httpkit.server :as http]
   [com.fulcrologic.fulcro.server.api-middleware :as server]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [com.stuartsierra.component :as component]
   [rp.syringe.component.http-common :as http-common]))

(def ^:private handler
  (fn [req]
    (case (:uri req)
      "/" {:status  302
           :headers {"Location" "/index.html"}}
      {:status  404
       :headers {"Content-Type" "text/plain"}
       :body    "Not Found"})))

(defn middleware
  [db-node]
  (-> handler
    (server/wrap-api {:uri    "/api"
                      :parser (partial parser/api-parser db-node)})
    (server/wrap-transit-params)
    (server/wrap-transit-response)
    (wrap-resource "public")
    wrap-content-type))

(defrecord HttpServerComponent []
  component/Lifecycle
  (start [{:keys [port crux] :as component}]
    (let [port (http-common/parse-maybe-int port)
          full-handler (-> (middleware (:node crux))
                           ;; Add ops, etc
                           (http-common/required-routes component))]
      (assoc component :stop-fn (http/run-server full-handler {:port port}))))
  (stop [component]
    (when-let [stop-fn (:stop-fn component)]
      (stop-fn))
    (dissoc component :stop-fn)))
