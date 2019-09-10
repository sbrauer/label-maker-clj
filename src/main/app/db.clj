(ns app.db
  (:require [crux.api :as crux]
            [taoensso.timbre :as log])
  (:import [crux.api ICruxAPI]))

(defn start-cluster-node
  ^crux.api.ICruxAPI
  ([rocks?]
   ;; FIXME: don't hardcode config
   (crux/start-cluster-node {:bootstrap-servers "localhost:9092"
                             :kv-backend (if rocks? "crux.kv.rocksdb.RocksKv" "crux.kv.memdb.MemKv")
                             :db-dir "data/db-dir-cluster"
                             :event-log-dir "data/eventlog-cluster"
                             :tx-topic "label-maker-tx"
                             :doc-topic "label-maker-doc"}))
  ([]
  ;; Note that we're using in-memory KV as default for now since RocksKv seems flaky with cluster node.
   (start-cluster-node false)))

;;
;; Convenience functions
;;

(defn put-docs!
  [node docs]
  (log/info "Putting crux docs:" docs)
  (crux/submit-tx node
                  (vec (for [doc docs]
                         [:crux.tx/put doc]))))

(defn entity
  [node entity-id]
  (log/info "Getting crux entity:" entity-id)
  (crux/entity (crux/db node) entity-id))

(defn q
  [node query]
  (log/info "Running crux query:" query)
  (crux/q (crux/db node) query))

(defn full-query
  "Get _everything_; handy for dev troubleshooting."
  [node]
  (q node
     '{:find [id]
       :where [[e :crux.db/id id]]
       :full-results? true}))

(defn entity-merge!
  "Merge a map of new-attrs into the specified entity."
  [node entity-id new-attrs]
  (let [prev (entity node entity-id)]
    (put-docs! node [(merge prev new-attrs)])))

(defn entity-update!
  "Update a single key of the specified entity, similar to `clojure.core/update`"
  [node entity-id k f & rest]
  (let [prev (entity node entity-id)]
    (put-docs! node [(apply update (concat [prev k f] rest))])))

(defn replace-crux-id
  "Given a doc, replace the generic crux ID field with the given ID keyword."
  [id-kw doc]
  (let [crux-id-kw :crux.db/id]
    (-> doc
        (assoc id-kw (crux-id-kw doc))
        (dissoc crux-id-kw))))

(defn tuple->map
  "Assign keys to a tuple of values."
  [ks vs]
  (apply hash-map (interleave ks vs)))

;;
;; Domain-specific query functions
;;

(defn text-sets
  "Get all text sets"
  [node]
  (map (partial tuple->map [:text-set/id :text-set/name])
       (q node
          '{:find [id name]
            :where [[id :text-set/name name]]
            :order-by [[id :asc]]})))

(defn text-ids-for-set-id
  "Get all text IDs for a specific set ID"
  [node set-id]
  (map first
       (q node {:find '[id]
                :where '[[e :crux.db/id id]
                         [e :text-set/id sid]]
                :args [{'sid set-id}]
                :order-by '[[id :asc]]})))

(defn text-for-id
  "Get text doc for a given ID"
  [node text-id]
  (replace-crux-id :text/id (entity node text-id)))

;;
;; Domain-specific functions with side effects!
;; FIXME: Are these too naive? Should they do validation or even use cas?

(defn add-phrase!
  [node text-id pos label-id]
  (entity-update! node text-id :text/phrases
                  conj
                  {:phrase/pos pos :phrase/label label-id}))

(defn remove-phrase!
  [node text-id pos]
  (entity-update! node text-id :text/phrases
                  (partial remove #(= pos (:phrase/pos %)))))

(comment
  (require '[clj-uuid :as uuid])
  (def sample-sets [{:crux.db/id (uuid/v1)
                     :text-set/name "Example Text Set"}
                    {:crux.db/id (uuid/v1)
                     :text-set/name "Another Example Text Set"}])
  (def set1-id (:crux.db/id (first sample-sets)))
  (def set2-id (:crux.db/id (second sample-sets)))
  (def sample-texts [{:crux.db/id (uuid/v1)
                      :text-set/id set1-id
                      :text/raw "2bd 2ba loft atl ga under 1500 for a family of 3"
                      :text/phrases #{{:phrase/pos [ 0  3] :phrase/label :beds}
                                      {:phrase/pos [ 4  7] :phrase/label :baths}
                                      {:phrase/pos [ 8 12] :phrase/label :ptype}
                                      {:phrase/pos [13 19] :phrase/label :cityst}
                                      {:phrase/pos [20 30] :phrase/label :price-lte}}}
                     {:crux.db/id (uuid/v1)
                      :text-set/id set1-id
                      :text/raw "Hello World"
                      :text/phrases #{}}
                     {:crux.db/id (uuid/v1)
                      :text-set/id set1-id
                      :text/raw "the quick brown fox jumps over the lazy dog"
                      :text/phrases #{}}])
  (defn seed-db!
    [node]
    (put-docs! node (concat sample-sets sample-texts)))

  (defn add-lots-of-texts
    [node]
    ;; Let's generate a bunch of texts for set 2 to see how the UI behaves with lots of text items.
    (put-docs! node (for [x (range 2000)]
                      {:crux.db/id (uuid/v1)
                       :text-set/id set2-id
                       :text/raw "the quick brown fox jumps over the lazy dog"
                       :text/phrases #{}})))

  (def node (start-cluster-node))

  (seed-db! node)
  (.close node)

  ;; Example of using the db-node in a running system
  (seed-db! (:db-node @user/sys))

  )
