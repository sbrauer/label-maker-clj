(ns app.mutations
  (:require
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

(defmutation delete-phrase
  [{text-id :text/id phrase-pos :phrase/pos}]
  (action [{:keys [state]}]
          (swap! state update-in [:text/id text-id :text/phrases] (fn [phrases] (vec (remove #(= phrase-pos (:phrase/pos %)) phrases)))))
  (remote [env] true))

(defmutation add-phrase
  [{text-id :text/id pos :phrase/pos label-id :label/id}]
  (action [{:keys [state]}]
          (swap! state update-in [:text/id text-id :text/phrases] conj {:phrase/pos pos :phrase/label [:label/id label-id]}))
  (remote [env] true))
