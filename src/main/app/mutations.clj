(ns app.mutations
  (:require
    [app.resolvers :as res]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation delete-qp
  [env {qp-id :qp/id q-id :q/id}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `delete-qp}
  (log/info "Deleting query part" qp-id "from query" q-id)
  ;; Remove the QP from the Q
  (swap! res/q-table update q-id update :q/parts (fn [old-list] (filterv #(not= qp-id %) old-list)))
  ;; Remove the QP
  (swap! res/qp-table dissoc qp-id))

(pc/defmutation add-qp
  [env {q-id :q/id pos :qp/pos label-id :label/id :keys [tempid]}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `add-qp}
  (log/info "Adding query part to query" q-id "at pos" pos "for label" label-id)
  (let [qp-id (res/next-qp-seq!)]
    ;; Add the new QP
    (swap! res/qp-table assoc qp-id {:qp/id qp-id :qp/pos pos :qp/label label-id})
    ;; Add the new QP to the Q
    (swap! res/q-table update q-id update :q/parts conj qp-id)
    {:tempids {tempid qp-id}}))

(def mutations [delete-qp add-qp])
