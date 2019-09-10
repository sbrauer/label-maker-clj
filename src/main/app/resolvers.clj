(ns app.resolvers
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [app.db :as db]))

;; FIXME: store labels in crux as well?
(def label-table
  (atom
   {:beds      {:label/id :beds      :label/color "#2452b5"}
    :baths     {:label/id :baths     :label/color "#0f99bf"}
    :ptype     {:label/id :ptype     :label/color "#d478f9"}
    :cityst    {:label/id :cityst    :label/color "#d6b3f2"}
    :price-lte {:label/id :price-lte :label/color "#53f237"}}))

(pc/defresolver label-resolver [env {:label/keys [id]}]
  {::pc/input  #{:label/id}
   ::pc/output [:label/color]}
  (get @label-table id))

(defn id->ident-map
  [k id]
  {k id})

(defn mapify-phrase-label-ids
  "Given a map representing a text entity/doc, replace each bare label id
  with {:label/id id} (which can be resolved by the label-resolver)"
  [text]
  (update text
          :text/phrases
          (fn [phrases]
            (map #(update % :phrase/label (partial id->ident-map :label/id)) phrases))))

(pc/defresolver text-resolver [env {:text/keys [id]}]
  {::pc/input  #{:text/id}
   ::pc/output [:text/raw {:text/phrases [:phrase/pos {:phrase/label [:label/id]}]}]}
  (when-let [text (db/text-for-id (:db-node env) id)]
    (mapify-phrase-label-ids text)))

(pc/defresolver text-set-resolver [env {:text-set/keys [id]}]
  {::pc/input  #{:text-set/id}
   ::pc/output [:text-set/name {:text-set/texts [:text/id]}]}
  (assoc (select-keys (db/text-for-id (:db-node env) id) [:text-set/name])
         :text-set/texts (map (partial id->ident-map :text/id) (db/text-ids-for-set-id (:db-node env) id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global resolvers
(pc/defresolver text-sets-resolver [env input]
  {::pc/output [{:text-sets [:text-set/id :text-set/name]}]}
  {:text-sets (db/text-sets (:db-node env))})

(pc/defresolver labels-resolver [env input]
  {::pc/output [{:labels [:label/id]}]}
  {:labels (map (fn [id] {:label/id id}) (keys @label-table))})

(def resolvers [label-resolver
                text-set-resolver
                text-resolver
                ;; Global resolvers
                labels-resolver
                text-sets-resolver])
