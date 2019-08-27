(ns app.mutations
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]))

(defmutation delete-qp
  [{q-id :q/id qp-id :qp/id}]
  (action [{:keys [state]}]
          (swap! state merge/remove-ident* [:qp/id qp-id] [:q/id q-id :q/parts]))
  (remote [env] true))
