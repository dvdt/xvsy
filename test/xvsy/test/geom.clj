(ns xvsy.test.geom
  (:require [xvsy.geom :refer :all]
            [xvsy.core]
            [clojure.math.combinatorics :refer :all]
            [clojure.test :refer :all]))

(def sample-layer-data
  (map (fn [[x y z]]
         {:x x :y y :z z})
       (cartesian-product  [:a :b :c] [:alpha :beta] [2 1 3])))

(deftest test-dodge
  (let [dodged (dodge sample-layer-data [:x :y])]
    (is (sequential? (-> dodged first :x))
        "Dodging on :x should make :x have multiple values")
    (is (= (set (cartesian-product [:a :b :c] [:alpha :beta]))
           (-> (map :x dodged) set)))))

(deftest test-stack
  (let [stacked (stack sample-layer-data :x :z)]
    (is (= (list [0 2] [2 3] [3 6] [6 8] [8 9] [9 12])
           (map :z (take 6 stacked)))))
  (is (= [{:x 1}] (stack [{:x 1}] :x nil))))

(def sample-bar-data
  (map (fn [[x fill y]]
         {:x x :fill fill :y y})
       (cartesian-product  [:a :b :c] [:alpha :beta] [3])))

(deftest bar-to-geom-attrs
  (testing "simple"
    (let [aes-mappings {:x {:col {:name :x1} :stat {:name :id}}
                        :y {:col {:name :y1} :stat {:name :count}}
                        :fill {:col {:name :f1} :stat {:name :id}}}
          stack-fill (adj-position (->Bar :x []) sample-bar-data)
          dodge-fill (adj-position (->Bar :x [[:fill :dodge]]) sample-bar-data)]
      (is (= '([0 3] [3 6] [0 3] [3 6]) (map :y (take 4 stack-fill))))
      (is (= '([0 3] [0 3]) (map :y (take 2 dodge-fill))))))
  (testing "arithmetic on nils is handled gracefully"
    (let [nil-data [{:x :a :fill :m :y 1}
                    {:x :a :fill :n :y 2}
                    {:x :a :fill :o :y nil}]
          stacked-data (adj-position (->Bar :x [[:fill :stack]]) nil-data)]
      (is (= (map :y stacked-data) [[0 1] [1 3] nil])))))

(deftest adj-position-bar
  (testing "stacking ignore nil values"
    (let [geom (->Bar :x [[:fill :stack]])
          adjusted-postions
          (adj-position geom [{:x 1 :fill 1 :y 1}
                              {:x 1 :fill 2 :y nil}
                              {:x 1 :fill 3 :y 3}])]
      (is (-> (group-by :fill adjusted-postions) (get 2) :y nil?)))))

;; test what happens when you try to position a geom that has nils in it.
;; arithmatic workflow must be aware of nils and handle them gracefully.
;; also factor workflow must handle nils gracefully
