(ns conexp.scripts.titanic-test
  (:require [clojure.test :refer [deftest]]
            [conexp.base :refer :all]
            [conexp.fca.algorithms.titanic :refer :all]
            [conexp.fca.contexts :refer :all]
            [conexp.fca.implications :refer :all]))

;;;

(def- testing-data
  [(diag-context (set-of-range 10)),
   (one-context (set-of-range 10)),
   (make-context #{1 2 3 4 5 6 7 8 9 10}
                 '#{e p c l i}
                 '#{[1 e] [1 c]
                    [2 e] [2 c] [2 i]
                    [3 e] [3 l] [3 i]
                    [4 e] [4 l] [4 i]
                    [5 e] [5 c] [5 i]
                    [6 e] [6 c]
                    [7 p] [7 l] [7 i]
                    [8 p] [8 l] [8 i]
                    [9 p] [9 l] [9 i]
                    [10 p] [10 l]}),
   ])

(deftest test-titanic-intents
  (with-testing-data [ctx testing-data]
    (= (set (intents ctx)) (set (titanic-intents ctx)))))

(deftest test-titanic-iceberg-intent-seq
  (forall [minsupp [0.0 0.2 0.5 0.7 0.9 1.0]]
    (with-testing-data [ctx testing-data]
      (= (set (frequent-closed-itemsets ctx minsupp))
         (set (titanic-iceberg-intent-seq ctx minsupp)))))
  (with-testing-data [ctx testing-data]
    (= (set (titanic-intents ctx))
       (set (titanic-iceberg-intent-seq ctx 0.0)))))

(deftest test-titanic-keys
  (with-testing-data [ctx testing-data]
    (= (set-of (context-attribute-closure ctx key)
               [key (titanic-keys (attributes ctx)
                                  (supports ctx 0)
                                  1.0
                                  <=)])
       (set (titanic-intents ctx)))))

(deftest test-supports
  (with-testing-data [ctx testing-data,
                      min [0 0.2 0.4 0.7 1.0]]
    (let [minnum (* (count (objects ctx)) min),
          sp     (supports ctx min),
          supps  (sp (subsets (attributes ctx)))]
      (forall [[atts supp] supps]
        (let [card (count (attribute-derivation ctx atts))]
          (if (<= minnum card)
            (= supp card)
            (= supp -1)))))))

;;;

nil
