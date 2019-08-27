(ns app.mutations
  (:require
    [app.resolvers :refer [q-table]]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation delete-qp
  [env {qp-id :qp/id q-id :q/id}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `delete-qp}
  (log/info "Deleting query part" qp-id "from query" q-id)
  (swap! q-table update q-id update :q/parts (fn [old-list] (filterv #(not= qp-id %) old-list))))

(def mutations [delete-qp])
