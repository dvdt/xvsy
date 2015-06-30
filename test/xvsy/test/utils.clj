(ns xvsy.test.utils
  (:require [clojure.test :refer :all]
            [xvsy.utils :refer :all]))

;; TODO: make this a real test or delete it
(deftest test-nums->str
  (let [small-nums (range 0.0 1e-4 1.5e-5)
        med-nums (range 0 10 1.5)
        large-nums (range 0 100.1 15.5)
        xl-nums (range 1e6 1e7 1100000)
        nums small-nums]
    (nums->str small-nums)
    (nums->str med-nums)
    (nums->str large-nums)
    (nums->str xl-nums)))


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
