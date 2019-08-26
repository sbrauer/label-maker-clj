(ns app.resolvers
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))

(defn analyze
  "Given an input string and a sequence of labeled parts (each with a label and a 2-tuple of position indices), return an ordered sequence of all parts (both labeled and unlabeled) of the string."
  [input parts]
  (let [labeled-positions (transduce (map (fn [{:qp/keys [pos label]}]
                                            (let [[start end] pos
                                                  {:label/keys [id]} label
                                                  substr (subs input start end)]
                                              {:str substr :label id :pos pos})))
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

(def label-table
  (atom
   {:beds      {:label/id :beds      :label/color "#2452b5"}
    :baths     {:label/id :baths     :label/color "#0f99bf"}
    :ptype     {:label/id :ptype     :label/color "#d478f9"}
    :cityst    {:label/id :cityst    :label/color "#d6b3f2"}
    :price-lte {:label/id :price-lte :label/color "#53f237"}}))

;; "Query Part"
(def qp-table
  (atom
   {1 {:qp/id 1 :qp/pos [ 0  3] :qp/label :beds}
    2 {:qp/id 2 :qp/pos [ 4  7] :qp/label :baths}
    3 {:qp/id 3 :qp/pos [ 8 12] :qp/label :ptype}
    4 {:qp/id 4 :qp/pos [13 19] :qp/label :cityst}
    5 {:qp/id 5 :qp/pos [20 30] :qp/label :price-lte}}))

(def q-table
  (atom
   {1 {:q/id 1
       :q/input "2bd 2ba loft atl ga under 1500 for a family of 3"
       :q/parts [1 2 3 4 5]}}))

(def qset-table
  (atom
   {:test {:qset/id :test
           :qset/name "Batch-002"
           :qset/queries [1]}}))

(pc/defresolver label-resolver [env {:label/keys [id]}]
  {::pc/input  #{:label/id}
   ::pc/output [:label/color]}
  (get @label-table id))

(pc/defresolver qp-resolver [env {:qp/keys [id]}]
  {::pc/input  #{:qp/id}
   ::pc/output [:qp/pos {:qp/label [:label/id]}]}
  (when-let [qp (get @qp-table id)]
    (update qp :qp/label (fn [id] {:label/id id}))))

(pc/defresolver q-resolver [env {:q/keys [id]}]
  {::pc/input  #{:q/id}
   ::pc/output [:q/input {:q/parts [:qp/id]}]}
  (when-let [q (get @q-table id)]
    ;; FIXME: Around here is where we could split the input string up into a collection of chunks (FIXME: better name) where each chunk has an _optional_ label.
    (update q :q/parts (partial mapv (fn [id] {:qp/id id})))))

(pc/defresolver qset-resolver [env {:qset/keys [id]}]
  {::pc/input  #{:qset/id}
   ::pc/output [:qset/name {:qset/queries [:q/id]}]}
  (when-let [qset (get @qset-table id)]
    (update qset :qset/queries (partial mapv (fn [id] {:q/id id})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global resolvers
(pc/defresolver queries-resolver [env input]
  {::pc/output [{:queries [:qset/id]}]}
  {:queries {:qset/id :test}})

(pc/defresolver labels-resolver [env input]
  {::pc/output [{:labels [:label/id]}]}
  {:labels (map (fn [id] {:label/id id}) (keys @label-table))})

(def resolvers [queries-resolver
                qset-resolver
                q-resolver
                qp-resolver
                label-resolver
                labels-resolver])