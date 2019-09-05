(ns app.mutations
  (:require
    [app.db :as db]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(pc/defmutation delete-phrase
  [env {pos :phrase/pos text-id :text/id}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `delete-phrase}
  (log/info "Deleting phrase" pos "from text" text-id)
  (db/remove-phrase! (:db-node env) text-id pos))

(pc/defmutation add-phrase
  [env {text-id :text/id pos :phrase/pos label-id :label/id}]
  ;; optional, this is how you override what symbol it responds to.  Defaults to current ns.
  {::pc/sym `add-phrase}
  (log/info "Adding phrase to text" text-id "at pos" pos "for label" label-id)
  (db/add-phrase! (:db-node env) text-id pos label-id))

(def mutations [delete-phrase add-phrase])
