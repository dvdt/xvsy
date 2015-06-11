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
