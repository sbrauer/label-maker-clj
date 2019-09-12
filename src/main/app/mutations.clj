(ns app.mutations
  (:require
    [app.db :as db]
    [com.wsscode.pathom.connect :as pc]
    [taoensso.timbre :as log]))

(defn update-phrases!
  [db-node text-id ui-prev-phrases f & args]
  ;; Prepare entity docs for a CAS...
  (let [prev-doc (-> (db/entity db-node text-id)
                     (assoc :text/phrases
                            ;; Transform the prev-phrases from the UI into the right shape for our crux documents.
                            ;; Replace `[:label/id id]` with bare `id`.
                            (set (map #(update % :phrase/label second) ui-prev-phrases))))
        new-doc (apply update (concat [prev-doc :text/phrases f] args))]
    (if (db/compare-and-swap! db-node prev-doc new-doc)
      :ok
      {:error {:type :conflict-error
               :current-text (db/text-for-id db-node text-id)}})))

(pc/defmutation delete-phrase
  [{:keys [db-node] :as env} {pos :phrase/pos text-id :text/id :keys [prev-phrases]}]
  {::pc/sym `delete-phrase}
  (log/info "Deleting phrase" pos "from text" text-id)
  (update-phrases! db-node text-id prev-phrases
                   (fn [phrases]
                     (set (remove #(= pos (:phrase/pos %)) phrases)))))

(pc/defmutation add-phrase
  [{:keys [db-node] :as env} {text-id :text/id pos :phrase/pos label-id :label/id :keys [prev-phrases]}]
  {::pc/sym `add-phrase}
  (log/info "Adding phrase to text" text-id "at pos" pos "for label" label-id)
  (update-phrases! db-node text-id prev-phrases
                   conj
                   {:phrase/pos pos :phrase/label label-id}))

(def mutations [delete-phrase add-phrase])
