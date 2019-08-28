(ns app.mutations
  (:require
    [app.resolvers :as res]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation delete-phrase
  [env {phrase-id :phrase/id text-id :text/id}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `delete-phrase}
  (log/info "Deleting phrase" phrase-id "from text" text-id)
  ;; Remove the QP from the Q
  (swap! res/text-table update text-id update :text/phrases (fn [old-list] (filterv #(not= phrase-id %) old-list)))
  ;; Remove the QP
  (swap! res/phrase-table dissoc phrase-id))

(pc/defmutation add-phrase
  [env {text-id :text/id pos :phrase/pos label-id :label/id :keys [tempid]}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `add-phrase}
  (log/info "Adding phrase to text" text-id "at pos" pos "for label" label-id)
  (let [phrase-id (res/next-seq! res/phrase-seq)]
    ;; Add the new QP
    (swap! res/phrase-table assoc phrase-id {:phrase/id phrase-id :phrase/pos pos :phrase/label label-id})
    ;; Add the new QP to the Q
    (swap! res/text-table update text-id update :text/phrases conj phrase-id)
    {:tempids {tempid phrase-id}}))

(def mutations [delete-phrase add-phrase])
