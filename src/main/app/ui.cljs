(ns app.ui
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [cljs.pprint :as pp]
            [clojure.string :as str]))

(defsc Label [this {:label/keys [id color]}]
  {:query [:label/id :label/color]
   :ident :label/id}
  (dom/li
   (dom/div :.label
            {:style {:background-color color}}
            (pr-str #:label{:id id :color color}))))

(def ui-label (comp/factory Label {:keyfn :label/id}))

(defsc QP [this {:qp/keys [id pos label]}]
  {:query [:qp/id :qp/pos {:qp/label (comp/get-query Label)}]
   :ident :qp/id})

#_(def ui-qp (com/factory QP))

(defn analyze
  [input parts]
  (let [schema (into (sorted-map)
                     (map (juxt :qp/pos :qp/label))
                     parts)
        _ (prn [:schema schema])
        span  (into (sorted-set)
                    (mapcat (partial apply range)) (keys schema))
        output (str/trim
                (apply str
                       (keep-indexed (fn [i c]
                                       (when-not (contains? span i) c))
                                     input)))]
    {:input input
     :output output
     :labels (transduce (map (fn [[[start end :as pos]
                                   {:label/keys [id color]} :as arg]]
                               (prn :arg arg)
                               (let [substr (subs input start end)]
                                 {:str substr :label id :pos pos})))
                        conj
                        []
                        schema)}))

(defsc Q [this {:q/keys [id input parts]}]
  {:query [:q/id :q/input {:q/parts (comp/get-query QP)}]
   :ident :q/id}
  (let [{:keys [output labels]} (analyze input parts)]
   (dom/div
    (dom/h4 (str "QueryID: " id))
    (dom/h4 (str "Input: " input))
    (dom/h4 (str "Output: " output))
    (dom/pre (with-out-str (pp/print-table labels))))))

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
