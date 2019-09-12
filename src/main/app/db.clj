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
  [node eid]
  (log/info "Getting crux entity:" eid)
  (crux/entity (crux/db node) eid))

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
  [node eid new-attrs]
  (let [prev (entity node eid)]
    (put-docs! node [(merge prev new-attrs)])))

(defn entity-update!
  "Update a single key of the specified entity, similar to `clojure.core/update`"
  [node eid k f & args]
  (let [prev (entity node eid)]
    (put-docs! node [(apply update (concat [prev k f] args))])))

(defn compare-and-swap!
  "Performs a cas transaction, returning a boolean to indicate success status."
  [node prev-doc new-doc]
  (let [eid (:crux.db/id prev-doc)
        tx (crux/submit-tx node [[:crux.tx/cas prev-doc new-doc]])
        _ (crux/sync node (:crux.tx/tx-time tx) nil)
        success? (crux/submitted-tx-updated-entity? node tx eid)]
    (log/info "Compare-and-swap!" {:prev-doc prev-doc :new-doc new-doc :success? success?})
    success?))

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

(comment
  (require '[clj-uuid :as uuid])

  (defn seed-db!
    [node]
    (let [set-id (uuid/v1)]
      (put-docs! node [{:crux.db/id set-id
                        :text-set/name "Example Text Set"}
                       {:crux.db/id (uuid/v1)
                        :text-set/id set-id
                        :text/raw "2bd 2ba loft atl ga under 1500 for a family of 3"
                        :text/phrases #{{:phrase/pos [ 0  3] :phrase/label :beds}
                                        {:phrase/pos [ 4  7] :phrase/label :baths}
                                        {:phrase/pos [ 8 12] :phrase/label :ptype}
                                        {:phrase/pos [13 19] :phrase/label :cityst}
                                        {:phrase/pos [20 30] :phrase/label :price-lte}}}
                       {:crux.db/id (uuid/v1)
                        :text-set/id set-id
                        :text/raw "Hello World"
                        :text/phrases #{}}
                       {:crux.db/id (uuid/v1)
                        :text-set/id set-id
                        :text/raw "the quick brown fox jumps over the lazy dog"
                        :text/phrases #{}}])))

  (defn seed-db2!
    [node]
    ;; Let's generate a set with a bunch of texts to see how the UI behaves with lots of text items.
    (let [set-id (uuid/v1)]
      (put-docs! node [{:crux.db/id set-id
                        :text-set/name "Another Example Text Set"}])
      (put-docs! node (for [x (range 2000)]
                        {:crux.db/id (uuid/v1)
                         :text-set/id set-id
                         :text/raw "the quick brown fox jumps over the lazy dog"
                         :text/phrases #{}}))))

  (def node (start-cluster-node))

  (seed-db! node)
  (.close node)

  ;; Example of using the db-node in a running system
  (seed-db! (:db-node @user/sys))

  (full-query (:db-node @user/sys))
  )
