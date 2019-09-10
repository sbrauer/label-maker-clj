(ns app.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [app.ui :as ui]))

(defonce app (app/fulcro-app
              {:remotes {:remote (http/fulcro-http-remote {})}
               :client-did-mount ui/client-did-mount}))
