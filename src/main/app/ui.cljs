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

(defn maybe-label-selection
  [component label-id]
  (fn []
    (let [selection (.getSelection js/window)]
      (when-not (str/blank? (str selection))
        (let [anchor-node (.-anchorNode selection)
              focus-node (.-focusNode selection)
              parent-el (.-parentElement focus-node)
              parent-data (.-dataset parent-el)]
          (when (and (= anchor-node focus-node)
                     (= "span" (-> parent-el .-tagName str/lower-case))
                     (= "phrase" (.-className parent-el)))
            (when-let [phrase-start (some-> parent-data .-start int)]
              (let [[sel-start sel-end] (sort [(.-anchorOffset selection) (.-focusOffset selection)])
                    pos [(+ phrase-start sel-start) (+ phrase-start sel-end)]]
                (comp/transact! component
                                [(api/add-phrase {:text/id (-> parent-data .-textId int)
                                                  :phrase/pos pos
                                                  :label/id label-id
                                                  :tempid (tmp/tempid)})])))))))))

(defsc Label [this {:label/keys [id color]}]
  {:query [:label/id :label/color]
   :ident :label/id}
  (dom/li
   (dom/button {:style {:backgroundColor color}
                :onClick (maybe-label-selection this id)
                :title "Apply label to selected phrase"} (name id))))

(def ui-label (comp/factory Label {:keyfn :label/id}))

;; FIXME: This is unused; kill or try to merge in render-phrase ?
(defsc Phrase [this {:phrase/keys [id pos label] :as FIXME}]
  {:query [:phrase/id :phrase/pos {:phrase/label (comp/get-query Label)}]
   :ident :phrase/id}
  ;;(dom/span (str FIXME))
  )
;;(def ui-phrase (comp/factory Phrase {:keyfn (comp first :phrase/pos)}))

;; FIXME: should this be in the Phrase component?
(defn render-phrase
  [{:keys [onDelete text-id]} {:keys [pos str label id] :as phrase}]
  (let [[start end] pos]
    (dom/span :.phrase
              {:key start
               :data-start start
               :data-end end
               :data-text-id text-id
               :title (:label/id label)
               :className (when label "labeled")
               :style {:backgroundColor (:label/color label)}}
              str
              (when label
                (dom/button {:onClick #(onDelete id) :title "Remove label"} "x")))))

(defsc Text [this {:text/keys [id raw phrases]}]
  {:query [:text/id :text/raw {:text/phrases (comp/get-query Phrase)}]
   :ident :text/id}
  (let [delete-phrase (fn [phrase-id]
                    (comp/transact! this [(api/delete-phrase {:text/id id :phrase/id phrase-id})]))]
    (dom/div
     (dom/h4 (str "Text ID: " id))
     (dom/h4 (str "Raw: " raw))
     ;;(dom/pre (with-out-str (pp/print-table (analyze raw phrases))))
     (dom/div :.text-labeler-widget (map (partial render-phrase {:onDelete delete-phrase :text-id id}) (analyze raw phrases))))))

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
                       (dom/ul :.labels
                        (map ui-label labels))))
            (dom/div :.column.right
                     (dom/h3 "Text Sets")
                     (when text-sets (ui-text-set text-sets))))))
