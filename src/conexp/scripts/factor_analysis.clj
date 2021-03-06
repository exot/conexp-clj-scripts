;; Copyright ⓒ the conexp-clj developers; all rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.scripts.factor-analysis
  "Implements factorization algorithms for contexts."
  (:use conexp.base
        conexp.fca.contexts
        [conexp.scripts.fuzzy sets logics fca])
  (:import [java.util HashMap]))

;;; Interface

(defmulti factorize-context
  "Factorize context by given method. Note that the method determines
  whether the context is a formal context (as in the boolean case) or
  a many-valued one (as in the fuzzy case)."
  {:arglists '([method context & args])}
  (fn [method context & _] method))

(defmulti incidence-cover?
  "Returns true iff the given sequence of concepts cover the incidence
  relation of the given context."
  {:arglists '([method context concepts])}
  (fn [method context concepts] method))

;;; Boolean

(defn- clear-factors
  "From the given factors remove all which are not needed."
  [factors]
  (let [^HashMap marked (HashMap.)]
    (doseq [factor-tail (tails factors),
            :let [[A_i B_i] (first factor-tail)]
            [A_j B_j] (rest factor-tail)]
      (let [S (intersection (cross-product A_i B_i)
                            (cross-product A_j B_j))]
        (when-not (empty? S)
          (.put marked [A_i B_i]
                (into (.get marked [A_i B_i])
                      S))
          (.put marked [A_j B_j]
                (into (.get marked [A_j B_j])
                      S)))))
    (filter (fn [[A B]]
              (not= (cross-product A B)
                    (set (.get marked [A B]))))
            factors)))

(defn- object-concepts
  "Returns the object concepts of context."
  [context]
  (set-of [g-prime-prime, g-prime]
          [g (objects context)
           :let [g-prime (object-derivation context #{g}),
                 g-prime-prime (attribute-derivation context g-prime)]]))

(defn- attribute-concepts
  "Returns the attribute concepts of context."
  [context]
  (set-of [m-prime, m-prime-prime]
          [m (attributes context)
           :let [m-prime (attribute-derivation context #{m}),
                 m-prime-prime (object-derivation context m-prime)]]))

(defmethod factorize-context :boolean-full
  [_ context]
  (let [F (intersection (object-concepts context)
                        (attribute-concepts context)),
        S (difference (set (concepts context)) F),
        U (difference (incidence context)
                      (set-of [i j] [[C D] F, i C, j D]))]
    (loop [U U,
           S S,
           F F]
      (if-not (empty? U)
        (let [[C D] (apply max-key
                           (fn [[X Y]]
                             (count (intersection U (cross-product X Y))))
                           S)]
          (recur (difference U (cross-product C D))
                 (disj S [C D])
                 (conj F [C D])))
        (clear-factors F)))))

(defn- oplus-count
  "Implements |D oplus j|. D must be subset of the attributes of
  context, as must #{j}."
  [context U D j]
  (let [D+j (conj D j),
        D+j-prime (attribute-derivation context D+j),
        D+j-prime-prime (object-derivation context D+j-prime)]
    (count (intersection U (cross-product D+j-prime D+j-prime-prime)))))

(defmethod factorize-context :boolean-adaptive
  [_ context]
  (let [M (attributes context)]
    (loop [U (incidence context),
           F #{}]
      (if-not (empty? U)
        (let [D (loop [D #{},
                       V 0]
                  (let [j-s     (difference M D),
                        max-j   (apply max-key (partial oplus-count context U D) j-s),
                        max-val (int (oplus-count context U D max-j))]
                    (if (< V max-val)
                      (recur (context-attribute-closure context (conj D max-j))
                             max-val)
                      D))),
              C (attribute-derivation context D)]
          (recur (difference U (cross-product C D))
                 (conj F [C D])))
        (clear-factors F)))))

(defmethod incidence-cover? :boolean
  [_ context concepts]
  (= (incidence context)
     (set-of [g m] [[C D] concepts,
                    g C,
                    m D])))

;;; Many Valued (with t-norms)

(defn- fuzzy-oplus-a
  "Helper function to compute D\\oplus_a j."
  [context U D a j]
  (let [D+        (assoc D j a),
        D+down    (fuzzy-attribute-derivation context D+),
        D+down-up (fuzzy-object-derivation context D+down)]
    (set-of [k l] [[k l] U,
                   :when (>= (f-star (D+down k) (D+down-up l))
                             ((incidence context) [k l]))])))

(defn- find-maximal
  "Find a pair [m v], where m is an attribute and v in (0,1],
  such that the cardinality of the set returned by fuzzy-oplus-a is
  maximal. Returns the triple [m v count], where count is the
  aforementioned cardinality."
  [values context U D]
  (let [attributes (set-of m [[[_ m] _] (select-keys (incidence context) U)])] ;note: is not empty
    (let [attribute (atom nil),
          max-value (atom -1),
          fuzzyness (atom nil)]
      (doseq [m attributes
              v values
              :when (not (>= (D m) v))
              :let  [c (count (fuzzy-oplus-a context U D v m))]
              :when (< @max-value c)]
        (reset! max-value c)
        (reset! attribute m)
        (reset! fuzzyness v))
      [@attribute @fuzzyness @max-value])))

(defmethod factorize-context :fuzzy
  [_ context truth-values]
  (let [inz          (incidence context),
        find-maximal (partial find-maximal truth-values context)]
    (loop [U (set-of [g m] [g (objects context),
                            m (attributes context),
                            :when (not (zero? (inz [g m])))]),
           F #{}]
      (if (empty? U)
        F
        (let [D (loop [D           (make-fuzzy-set {}),
                       V           0,
                       [j a value] (find-maximal U D)]
                  (let [value (int value)]
                    (when (zero? value)
                      (illegal-state "Could not find next attribute (0 value). "
                                     "Make sure that no floating point inaccuracies occured."))
                    (if (> value V)
                      (recur (fuzzy-object-derivation context
                                                      (fuzzy-attribute-derivation context (assoc D j a)))
                             value
                             (find-maximal U D))
                      D))),
              C (fuzzy-attribute-derivation context D),
              F (conj F [C,D]),
              U (set-of [i j] [[i j] U,
                               :when (not (<= (inz [i j])
                                              (f-star (C i) (D j))))])]
          (recur U F))))))

;;;

nil
