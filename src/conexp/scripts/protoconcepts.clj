;; Copyright ⓒ the conexp-clj developers; all rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.scripts.protoconcepts
  "Add functionality for protoconcepts to conexp-clj."
  (:require [clojure.core :refer [set?]]
            [clojure.set :refer [subset?]]
            [clojure.test :refer [deftest is]]
            [conexp.base :refer [cross-product]]
            [conexp.fca.contexts
             :refer
             [adprime attributes concepts make-context objects oprime
              odprime]]))

(defn protoconcept?
  "Test whether [x y] is a protoconcept of the formal context `ctx'."
  [ctx [x y]]
  (and (set? x)
       (set? y)
       (subset? x (objects ctx))
       (subset? y (attributes ctx))
       (= (oprime ctx x)
          (adprime ctx y))))

(declare protoconcepts-generating-concept)

(defn protoconcepts
  "Return (lazy?) sequence of all protoconcepts of the formal context `ctx'."
  [ctx]
  (mapcat (partial protoconcepts-generating-concept ctx)
          (concepts ctx)))

(declare generators)

(defn- protoconcepts-generating-concept
  "For a formal concept [x y] of the formal context `ctx', returns all
  protoconcepts [a b] such that a' = x and b' = y."
  [ctx [x y]]
  (let [x-generators (distinct (generators #(= (odprime ctx %) x) x))
        y-generators (distinct (generators #(= (adprime ctx %) y) y))]
    (cross-product x-generators y-generators)))

(defn- generators
  "Find all subsets of x satisfying `pred', where `pred' is a monotone predicate
  with respect to ⊆."
  [pred x]
  (lazy-seq (if (not (pred x))
              nil
              (cons x (mapcat #(generators pred (disj x %)) x)))))

(def ^:private
  four-elements (make-context '#{water earth air fire}
                              '#{cold moist dry warm}
                              '#{[water cold] [water moist]
                                 [earth cold] [earth dry]
                                 [air moist] [air warm]
                                 [fire dry] [fire warm]}))

(deftest test-generators
  (is (= 29 (count (distinct (generators #(>= (count %) 5) #{1 2 3 4 5 6 7}))))))

(deftest test-protoconcepts
  (is (= 22 (count (protoconcepts four-elements)))))

;; TODO: generate layout for poset (ignore fact that it is a lattice!)
