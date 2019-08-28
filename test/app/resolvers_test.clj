;; FIXME: Move this test to proper NS and adjust to match current reality.
;; At some point `analyze` was moved to another NS and modified.
(ns app.resolvers-test
  (:require [clojure.test :refer :all]
            [app.resolvers :as sut]))

(deftest test-analyze
  (let [input "The quick brown fox jumps over the lazy dog"
        ;; Actual parts likely to be in a vector, but the order shouldn't matter, so using a set for this test.
        parts #{{:qp/id 1 :qp/pos   [4 9] :qp/label {:label/id :adjective}}
                {:qp/id 2 :qp/pos [10 15] :qp/label {:label/id :adjective}}
                {:qp/id 3 :qp/pos [16 19] :qp/label {:label/id :noun}}
                {:qp/id 2 :qp/pos [35 39] :qp/label {:label/id :adjective}}}]
    (is (= [{:pos   [0 4] :str "The "}
            {:pos   [4 9] :str "quick" :label :adjective}
            {:pos  [9 10] :str " "}
            {:pos [10 15] :str "brown" :label :adjective}
            {:pos [15 16] :str " "}
            {:pos [16 19] :str "fox" :label :noun}
            {:pos [19 35] :str " jumps over the "}
            {:pos [35 39] :str "lazy" :label :adjective}
            {:pos [39 43] :str " dog"}]
           (sut/analyze input parts)))))
