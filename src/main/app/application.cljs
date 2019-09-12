(ns app.application
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [app.ui :as ui]))

(defonce app (app/fulcro-app
              {:remotes {:remote (http/fulcro-http-remote {})}
               :client-did-mount ui/client-did-mount
               ;; Extend the default `remote-error?` to check for a custom `:error` key that we made up to allow remote mutations to indicate an error.
               :remote-error? (fn [{:keys [body] :as result}]
                                (or (app/default-remote-error? result)
                                    (some :error (vals body))))}))
