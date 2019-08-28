(ns app.resolvers
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))

(def label-table
  (atom
   {:beds      {:label/id :beds      :label/color "#2452b5"}
    :baths     {:label/id :baths     :label/color "#0f99bf"}
    :ptype     {:label/id :ptype     :label/color "#d478f9"}
    :cityst    {:label/id :cityst    :label/color "#d6b3f2"}
    :price-lte {:label/id :price-lte :label/color "#53f237"}}))

;; A "phrase" represents a substring of a text that has been labeled.
(def phrase-table
  (atom
   {1 {:phrase/id 1 :phrase/pos [ 0  3] :phrase/label :beds}
    2 {:phrase/id 2 :phrase/pos [ 4  7] :phrase/label :baths}
    3 {:phrase/id 3 :phrase/pos [ 8 12] :phrase/label :ptype}
    4 {:phrase/id 4 :phrase/pos [13 19] :phrase/label :cityst}
    5 {:phrase/id 5 :phrase/pos [20 30] :phrase/label :price-lte}}))

;; A RDBMS-inspired sequence for auto-incrementing IDs
(defn next-seq! [seq-atom]
  (swap! seq-atom inc))

(def phrase-seq (atom (apply max (conj (keys @phrase-table) 0)))) ;; init to max ID or zero

(def text-table
  (atom
   {1 {:text/id 1
       :text/raw "2bd 2ba loft atl ga under 1500 for a family of 3"
       :text/phrases [1 2 3 4 5]}}))

(def text-set-table
  (atom
   {:test {:text-set/id :test
           :text-set/name "Batch-001"
           :text-set/texts [1]}}))

(pc/defresolver label-resolver [env {:label/keys [id]}]
  {::pc/input  #{:label/id}
   ::pc/output [:label/color]}
  (get @label-table id))

(pc/defresolver phrase-resolver [env {:phrase/keys [id]}]
  {::pc/input  #{:phrase/id}
   ::pc/output [:phrase/pos {:phrase/label [:label/id]}]}
  (when-let [phrase (get @phrase-table id)]
    (update phrase :phrase/label (fn [id] {:label/id id}))))

(pc/defresolver text-resolver [env {:text/keys [id]}]
  {::pc/input  #{:text/id}
   ::pc/output [:text/raw {:text/phrases [:phrase/id]}]}
  (when-let [text (get @text-table id)]
    (update text :text/phrases (partial mapv (fn [id] {:phrase/id id})))))

(pc/defresolver text-set-resolver [env {:text-set/keys [id]}]
  {::pc/input  #{:text-set/id}
   ::pc/output [:text-set/name {:text-set/texts [:text/id]}]}
  (when-let [text-set (get @text-set-table id)]
    (update text-set :text-set/texts (partial mapv (fn [id] {:text/id id})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global resolvers
(pc/defresolver text-sets-resolver [env input]
  {::pc/output [{:text-sets [:text-set/id]}]}
  {:text-sets {:text-set/id :test}})

(pc/defresolver labels-resolver [env input]
  {::pc/output [{:labels [:label/id]}]}
  {:labels (map (fn [id] {:label/id id}) (keys @label-table))})

(def resolvers [label-resolver
                text-set-resolver
                text-resolver
                phrase-resolver
                ;; Global resolvers
                labels-resolver
                text-sets-resolver])
