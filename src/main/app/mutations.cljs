(ns app.mutations
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation delete-phrase
  [{text-id :text/id phrase-id :phrase/id}]
  (action [{:keys [state]}]
          ;; Remove the phrase from the text
          (swap! state merge/remove-ident* [:phrase/id phrase-id] [:text/id text-id :text/phrases])
          ;; Remove the phrase
          (swap! state update :phrase/id dissoc phrase-id))
  (remote [env] true))

(defmutation add-phrase
  [{text-id :text/id pos :phrase/pos label-id :label/id :keys [tempid]}]
  (action [{:keys [state]}]
          ;; Add the new phrase
          (swap! state assoc-in [:phrase/id tempid] {:phrase/id tempid :phrase/pos pos :phrase/label [:label/id label-id]})
          ;; Add the new phrase to the text
          (swap! state update-in [:text/id text-id :text/phrases] conj [:phrase/id tempid]))
  (remote [env] true))
