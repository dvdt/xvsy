(ns xvsy.test.stat
  (:require [clojure.test :refer :all]
            [xvsy.stat :refer :all]))

(def minimal-korma-query {:db {:subprotocol "postgresql"}})

(deftest test-bind-field
  (let [g (bind-field minimal-korma-query {:stat {:name :avg}
                                           :col {:name :my-col}})]
    (is (= 2 (count g)) "Korma fields are a map of two kv pairs")))

(deftest test-aggregate
  (is (true? (aggregator? {:stat {:name :avg}})))
  (is (false? (aggregator? {:stat {:name :asdfasdf}}))))

(deftest test-factor?
  (let [factor-col {:name :factor-col :factor true}
        factor-id {:stat {:name :id} :col factor-col}
        factor-bin (assoc-in factor-id [:stat :name] :bin)
        factor-count {:stat {:name :count} :col factor-col}]
    (is (true? (factor? factor-id))
        "data columns that are factor should remain factor after the id stat.")
    (is (true? (factor? factor-bin)))
    (is (false? (factor? factor-count))
        "Factor columns should be non-factor after the count stat")
    (is (false? (factor? (assoc-in factor-id [:col :factor] false)))
        "non-factor columns should remain non-factor, except for bin")
    (is (true? (factor? (assoc-in factor-bin [:col :factor] false)))
        "non-factor columns should remain non-factor, except for bin")
    (is (false? (factor? (assoc-in factor-count [:col :factor] false)))
        "non-factor columns should remain non-factor, except for bin")))
