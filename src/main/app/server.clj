(ns app.server
  (:require
   [app.parser :as parser]
   [org.httpkit.server :as http]
   [com.fulcrologic.fulcro.server.api-middleware :as server]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]))

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

(defn start [{:keys [db-node] :as sys}]
  (http/run-server (middleware db-node) {:port 3000}))
