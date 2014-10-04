(ns some.tests
  (use conexp.main))

(def testing-implications
  (vec
   (let [all (set (range 14))]
     (set-of (make-implication A all) | A (subsets all)))))

(defn go []
  (time (count (persistent!
                (reduce (fn [map i]
                          (reduce (fn [map m]
                                    (assoc! map m (conj (map m) i)))
                                  map
                                  (.premise ^conexp.fca.implications.Implication (testing-implications i))))
                        (transient {})
                        (range (count testing-implications)))))))

