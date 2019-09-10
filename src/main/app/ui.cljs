(ns app.ui
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [app.mutations :as api]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn analyze-text
  "Given a raw text string and a sequence of labeled phrases, return an ordered sequence of _all_ phrases (both labeled and unlabeled)."
  [raw phrases]
  (let [labeled-idxs (mapcat #(apply range (:phrase/pos %)) phrases)
        unlabeled-phrases (->> labeled-idxs
                               (reduce (fn [acc idx]
                                         (assoc acc idx nil))
                                       (vec raw))
                               (map-indexed vector)
                               (partition-by (comp some? second))
                               (filter (comp second first))
                               (map (fn [indexed-chars]
                                      {:phrase/pos [(first (first indexed-chars))
                                                    (inc (first (last indexed-chars)))]})))]
    (sort-by :phrase/pos (concat phrases unlabeled-phrases))))

(defn maybe-label-selection
  "Gets the current browser selection; when non-empty and inside a single unlabelled phrase span, labels the phrase with the given label ID."
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
                     ;; Note that an already labeled span would have the className "phrase labeled".
                     (= "phrase" (.-className parent-el)))
            (when-let [phrase-start (some-> parent-data .-start int)]
              (let [[sel-start sel-end] (sort [(.-anchorOffset selection) (.-focusOffset selection)])
                    pos [(+ phrase-start sel-start) (+ phrase-start sel-end)]]
                (comp/transact! component
                                [(api/add-phrase {:text/id (->> parent-data .-textId (keyword "text"))
                                                  :phrase/pos pos
                                                  :label/id label-id})])))))))))

(defsc Label [this {:label/keys [id color]}]
  {:query [:label/id :label/color]
   :ident :label/id}
  (dom/li
   (dom/button {:style {:backgroundColor color}
                :onClick (maybe-label-selection this id)
                :title "Apply label to selected phrase"} (name id))))

(def ui-label (comp/factory Label {:keyfn :label/id}))

(defsc Phrase [this
               {:phrase/keys [pos label]}
               ;; The next arg is a map of computed options from the parent component.
               {:keys [onDelete substr text-id]}]
  {:query [:phrase/pos {:phrase/label (comp/get-query Label)}]}
  (let [[start end] pos]
    (dom/span :.phrase
              {:key start
               :data-start start
               :data-end end
               :data-text-id text-id
               :title (:label/id label)
               :className (when label "labeled")
               :style {:backgroundColor (:label/color label)}}
              substr
              (when label
                (dom/button {:onClick #(onDelete pos) :title "Remove label"} "x")))))

(def ui-phrase (comp/factory Phrase {:keyfn (comp first :phrase/pos)}))

(defsc Text [this {:text/keys [id raw phrases]}]
  {:query [:text/id :text/raw {:text/phrases (comp/get-query Phrase)}]
   :ident :text/id}
  (let [delete-phrase (fn [phrase-pos]
                        (comp/transact! this [(api/delete-phrase {:text/id id :phrase/pos phrase-pos})]))]
    (dom/div
     (dom/h4 (str "Text ID: " id))
     (dom/p "Raw: " raw)
     (dom/div :.text-labeler-widget
              (map (fn [p]
                     (let [[start end] (:phrase/pos p)
                           computed {:onDelete delete-phrase
                                     :text-id id
                                     :substr (subs raw start end)}]
                       (ui-phrase (comp/computed p computed))))
                   (analyze-text raw phrases))))))

(def ui-text (comp/factory Text {:keyfn :text/id}))

(defsc TextSet [this {:text-set/keys [id name texts]}]
  {:query [:text-set/id :text-set/name {:text-set/texts (comp/get-query Text)}]
   :ident :text-set/id
   :route-segment ["text-set" :text-set/id]
   :will-enter (fn [app {:text-set/keys [id] :as route-params}]
                 ;;(log/info "Will enter text-set with route params " route-params)
                 ;; At this point `id` is a string representing the non-namespaced ID, so let's convert to a namespaced keyword.
                 (let [id (keyword "text-set" id)]
                   (dr/route-deferred [:text-set/id id]
                                      #(df/load app [:text-set/id id] TextSet
                                                {:post-mutation `dr/target-ready
                                                 :post-mutation-params
                                                 {:target [:text-set/id id]}}))))}
  (dom/div
   (dom/div :#crumbtrail
            (dom/a {:href "#" :onClick #(dr/change-route this ["main"])} "Home")
            " / "
            (str name "(" id ")"))
   (dom/h3 name)
   (map ui-text texts)))

(def ui-text-set (comp/factory TextSet {:keyfn :text-set/id}))

;; This component renders a link for navigating to a specific text-set route.
(defsc TextSetChoice [this {:text-set/keys [id name]}]
  {:query [:text-set/id :text-set/name]
   :ident :text-set/id}
  (dom/div
   (dom/li (dom/a {:href "#"
                   :onClick #(dr/change-route this ["text-set" (clojure.core/name id)])}
                  (str name " (" id ")")))))

(def ui-text-set-choice (comp/factory TextSetChoice {:keyfn :text-set/id}))

;; The Main component renders a list of text sets for the user to choose from.
(defsc Main [this {:keys [text-sets] :as props}]
  {:ident         (fn [] [:component/id ::main])
   :query         [:text-sets]
   :route-segment ["main"]
   :will-enter    (fn [app route-params]
                    ;;(log/info "Will enter main with route params " route-params)
                    (dr/route-deferred [:component/id ::main]
                                       #(df/load app :text-sets Main
                                                 {:post-mutation `dr/target-ready
                                                  :post-mutation-params
                                                  {:target [:component/id ::main]}})))}
  (dom/div
   (dom/h1 "Welcome to Label Maker!")
   (if text-sets
     (dom/div
      (dom/p "Please select a text set...")
      (dom/ul (map ui-text-set-choice text-sets)))
     (dom/p "No text sets found."))))

(defrouter TopRouter [this {:keys [current-state pending-path-segment]}]
  {:router-targets [Main TextSet]}
  (case current-state
    :pending (dom/img {:alt "Loading..."
                       :src "/img/spinner.gif"})
    :failed (dom/div "Oh dear... Loading seems to have failed.")
    (dom/div "Unknown route")))

(def ui-top-router (comp/factory TopRouter))

(defsc Root [this {:keys [labels :root/router]}]
  {:query [{:labels      (comp/get-query Label)}
           {:root/router (comp/get-query TopRouter)}]
   :initial-state {:root/router {}}}
  (dom/div :#container
           (dom/div :#sidebar
                    (dom/div :#sidebar-content
                             (dom/h2 "Label Maker")
                             (when labels
                               (dom/ul :.labels
                                       (map ui-label labels)))))
           (dom/div :#main
                    (dom/div :#main-content
                             (ui-top-router router)))))

(defn client-did-mount
  "Must be used as :client-did-mount parameter of app creation, or called just after you mount the app."
  [app]
  (dr/change-route app ["main"]))
