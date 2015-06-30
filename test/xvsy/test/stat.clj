(ns xvsy.test.stat
  (:require [clojure.test :refer :all]
            [xvsy.stat :refer :all]))

(def minimal-korma-query {:db {:subprotocol "postgresql"}})

(deftest test-bind-field
  (let [g (bind-field minimal-korma-query {:stat {:name :avg}
                                           :col {:name :my-col}})]
    (is (= 2 (count g)) "Korma fields are a map of two kv pairs")))
