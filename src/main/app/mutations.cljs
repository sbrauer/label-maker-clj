(ns app.mutations
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation delete-qp
  [{q-id :q/id qp-id :qp/id}]
  (action [{:keys [state]}]
          (swap! state merge/remove-ident* [:qp/id qp-id] [:q/id q-id :q/parts])
          (swap! state update :qp/id dissoc qp-id))
  (remote [env] true))

(defmutation add-qp
  ;; FIXME: really bother using namespaced keys? if not, consider rejiggering depete-qp as well
  [{q-id :q/id pos :qp/pos label-id :label/id :keys [tempid]}]
  (action [{:keys [state]}]
          ;; Add the new QP
          (swap! state assoc-in [:qp/id tempid] {:qp/id tempid :qp/pos pos :qp/label [:label/id label-id]})
          ;; Add the new QP to the Q
          (swap! state update-in [:q/id q-id :q/parts] conj [:qp/id tempid]))
  (remote [env] true))
