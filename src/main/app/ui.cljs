(ns app.ui
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.algorithms.tempid :as tmp]
            [app.mutations :as api]
            [cljs.pprint :as pp]
            [clojure.string :as str]))

(defn analyze
  "Given a raw text string and a sequence of labeled phrases, return an ordered sequence of _all_ phrases (both labeled and unlabeled)."
  [raw phrases]
  (let [labeled-positions (transduce (map (fn [{:phrase/keys [pos label id]}]
                                            (let [[start end] pos
                                                  substr (subs raw start end)]
                                              {:id id :str substr :label label :pos pos})))
                                     conj
                                     []
                                     phrases)
        labeled-idxs (into #{} (mapcat (partial apply range)) (map :phrase/pos phrases))
        unlabeled-positions (->> (map-indexed
                                  vector
                                  (reduce (fn [acc idx]
                                            (assoc acc idx nil))
                                          (vec raw)
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

;; FIXME: This is unused; kill or try to merge in present-phrase ?
(defsc Phrase [this {:phrase/keys [id pos label] :as FIXME}]
  {:query [:phrase/id :phrase/pos {:phrase/label (comp/get-query Label)}]
   :ident :phrase/id}
  ;;(dom/span (str FIXME))
  )
;;(def ui-phrase (comp/factory Phrase {:keyfn (comp first :phrase/pos)}))

;; FIXME: should this be in the Phrase component?
(defn present-phrase
  [{:keys [onDelete]} {:keys [pos str label id] :as phrase}]
  (let [[start end] pos]
    (dom/span :.phrase
              {:key start
               :data-start start
               :data-end end
               :title (:label/id label)
               :style {:backgroundColor (:label/color label)}}
              str
              (when label
                (dom/button {:onClick #(onDelete id) :title "Remove label"} "x")))))

(defn maybe-label-selection
  [component text-id]
  (fn []
    (let [selection (.getSelection js/window)]
      (when-not (str/blank? (str selection))
        (let [anchor-node (.-anchorNode selection)
              focus-node (.-focusNode selection)
              parent-el (.-parentElement focus-node)]
          (when (and (= anchor-node focus-node)
                     (= "span" (-> parent-el .-tagName str/lower-case))
                     (= "phrase" (.-className parent-el)))
            (when-let [phrase-start (some-> parent-el .-dataset .-start int)]
              (let [[sel-start sel-end] (sort [(.-anchorOffset selection) (.-focusOffset selection)])
                    pos [(+ phrase-start sel-start) (+ phrase-start sel-end)]]
                ;; FIXME: don't hardcode the label id...
                (comp/transact! component
                                [(api/add-phrase {:text/id text-id
                                                  :phrase/pos pos
                                                  :label/id :beds
                                                  :tempid (tmp/tempid)})])))))))))

(defsc Text [this {:text/keys [id raw phrases]}]
  {:query [:text/id :text/raw {:text/phrases (comp/get-query Phrase)}]
   :ident :text/id}
  (let [delete-phrase (fn [phrase-id]
                    (comp/transact! this [(api/delete-phrase {:text/id id :phrase/id phrase-id})]))]
    (dom/div
     (dom/h4 (str "Text ID: " id))
     (dom/h4 (str "Raw: " raw))
     ;;(dom/pre (with-out-str (pp/print-table (analyze raw phrases))))
     (dom/div :.text-labeler-widget (map (partial present-phrase {:onDelete delete-phrase}) (analyze raw phrases)))
     (dom/p (dom/button {:onClick (maybe-label-selection this id)} "label current selection")))))

(def ui-text (comp/factory Text {:keyfn :text/id}))

(defsc TextSet [this {:text-set/keys [id name texts]}]
  {:query [:text-set/id :text-set/name {:text-set/texts (comp/get-query Text)}]
   :ident :text-set/id}
  (dom/div
   (dom/h4 (str "Set ID: " id))
   (dom/h4 (str "Set Name: " name))
   (dom/ul (map ui-text texts))))

(def ui-text-set (comp/factory TextSet))

(defsc Root [this {:keys [labels text-sets]}]
  {:query [{:text-sets (comp/get-query TextSet)}
           {:labels    (comp/get-query Label)}]}
  (dom/div
   (dom/h2 "Label Maker")
   (dom/div :.row
            (dom/div :.column.left
                     (dom/h3 "Labels")
                     (when labels
                       (dom/ul
                        (map ui-label labels))))
            (dom/div :.column.right
                     (dom/h3 "Text Sets")
                     (when text-sets (ui-text-set text-sets))))))
