(ns app.ui
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [app.mutations :as api]
            [cljs.pprint :as pp]
            [clojure.string :as str]))

(defn analyze
  "Given an input string and a sequence of labeled parts (each with a label and a 2-tuple of position indices), return an ordered sequence of all parts (both labeled and unlabeled) of the string."
  [input parts]
  (let [labeled-positions (transduce (map (fn [{:qp/keys [pos label id]}]
                                            (let [[start end] pos
                                                  ;;{:label/keys [id]} label
                                                  substr (subs input start end)]
                                              {:id id :str substr :label label :pos pos})))
                                     conj
                                     []
                                     parts)
        labeled-idxs (into #{} (mapcat (partial apply range)) (map :qp/pos parts))
        unlabeled-positions (->> (map-indexed
                                  vector
                                  (reduce (fn [acc idx]
                                            (assoc acc idx nil))
                                          (vec input)
                                          labeled-idxs))
                                 (partition-by (comp some? second))
                                 (filter (comp second first))
                                 (map (fn [indexed-chars]
                                        {:pos [(first (first indexed-chars)) (inc (first (last indexed-chars)))] :str (apply str (map second indexed-chars))})))]
    (sort-by :pos (concat labeled-positions unlabeled-positions))))

(defsc Label [this {:label/keys [id color]}]
  {:query [:label/id :label/color]
   :ident :label/id}
  (dom/li
   (dom/div :.label
            {:style {:backgroundColor color}}
            (pr-str #:label{:id id :color color}))))

(def ui-label (comp/factory Label {:keyfn :label/id}))

(defsc QP [this {:qp/keys [id pos label] :as FIXME}]
  {:query [:qp/id :qp/pos {:qp/label (comp/get-query Label)}]
   :ident :qp/id}
  ;;(dom/span (str FIXME))
  )

;;(def ui-qp (comp/factory QP {:keyfn (comp first :qp/pos)}))

;; FIXME: should this be in a component (the QP component)?
(defn present-part
  [{:keys [onDelete]} {:keys [pos str label id] :as part}]
  (let [color (:label/color label)]
    ;; FIXME: remove unneeded attrs (originally added for debugging only)
    ;; FIXME: Add a delete control for labelled spans
    (dom/span :.part-label {:key (first pos) :data-start (first pos) :data-end (last pos) :data-label-id (:label/id label) :style {:backgroundColor color}}
              str
              (when label (dom/button {:onClick #(onDelete id)} "X")))))

(defsc Q [this {:q/keys [id input parts]}]
  {:query [:q/id :q/input {:q/parts (comp/get-query QP)}]
   :ident :q/id}
  (let [delete-qp (fn [qp-id]
                    (comp/transact! this [(api/delete-qp {:q/id id :qp/id qp-id})]))]
    (dom/div
     (dom/h4 (str "QueryID: " id))
     (dom/h4 (str "Input: " input))
     (dom/h4 "Editor (WIP):")
     ;;(dom/pre :.query-editor (with-out-str (pp/print-table (analyze input parts))))
     (dom/div :.query-editor (map (partial present-part {:onDelete delete-qp}) (analyze input parts))))))

(def ui-q (comp/factory Q {:keyfn :q/id}))

(defsc QSet [this {:qset/keys [id name queries]}]
  {:query [:qset/id :qset/name {:qset/queries (comp/get-query Q)}]
   :ident :qset/id}
  (dom/div
   (dom/h4 (pr-str #:qset{:id id :name name}))
   (dom/ul (map ui-q queries))))

(def ui-qset (comp/factory QSet))

(defsc Root [this {:keys [queries labels]}]
  {:query [{:queries (comp/get-query QSet)}
           {:labels  (comp/get-query Label)}]}
  (dom/div
   (dom/h2 "NLP")
   (dom/div :.row
            (dom/div :.column.left
                     (dom/h3 "Labels")
                     (when labels
                       (dom/ul
                        (map ui-label labels)))                     )
            (dom/div :.column.right
                     (dom/h3 "Queries")
                     (when queries (ui-qset queries))))))
