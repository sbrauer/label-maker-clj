(ns app.mutations
  (:require
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [taoensso.timbre :as log]))

(defn add-previous-phrases-to-remote-params
  [{:keys [ast state-before-action] :as env} text-id]
  ;; Add the previous state of the phrases to the params
  ;; so the remote mutation can do a compare-and-set.
  (let [phrases (get-in state-before-action [:text/id text-id :text/phrases])]
    (assoc-in ast [:params :prev-phrases] phrases)))

(defn handle-error
  [{:keys [app ref result state] :as env} text-id]
  (let [{:keys [error] :as mutation-result} (first (vals (:body result)))]
    (when (= :conflict-error (:type error))
      (js/alert "Write conflict detected. Will reload latest data from server.")
      (swap! state assoc-in [:text/id text-id :text/phrases]
             ;; Normalize the labels for fulcro
             (vec (map (fn [p]
                         (update p :phrase/label #(vector :label/id %)))
                       (get-in error [:current-text :text/phrases])))))))

(defmutation delete-phrase
  [{text-id :text/id phrase-pos :phrase/pos}]
  (action [{:keys [state]}]
          (swap! state update-in [:text/id text-id :text/phrases] (fn [phrases] (vec (remove #(= phrase-pos (:phrase/pos %)) phrases)))))
  (remote [env] (add-previous-phrases-to-remote-params env text-id))
  (error-action [env] (handle-error env text-id)))

(defmutation add-phrase
  [{text-id :text/id pos :phrase/pos label-id :label/id}]
  (action [{:keys [state]}]
          (swap! state update-in [:text/id text-id :text/phrases] conj {:phrase/pos pos :phrase/label [:label/id label-id]}))
  (remote [env] (add-previous-phrases-to-remote-params env text-id))
  (error-action [env] (handle-error env text-id)))
