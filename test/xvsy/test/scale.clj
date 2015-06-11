(ns xvsy.test.scale
  (:use clojure.test)
  (:use clojure.math.combinatorics)
  (:use xvsy.utils)
  (:require
   [xvsy.conf :as conf]
   [xvsy.stat :as stat]
   [xvsy.aesthetics :as aesthetics]
   [xvsy.scale :refer :all]))

(defn float= [a b] (< (Math/abs (- a b)) 0.000001))
(deftest interval-mins
  (is (= (interval-or-point-min-max [0 1 2]) [0 2]))
  (is (= (interval-or-point-min-max [[0 1] [1 2]]) [0 2])))

(deftest linear-t
  (is (float= 100 (linear [0 1] [0 100] 1)))
  (is (float= 0 (linear [0 1] [0 100] 0)))
  (is (float= 50 (linear [0 1] [0 100] 1/2))))

(def test-factor-data
  (let [cprod (cartesian-product [:x1 :x2] [:z1 :z2] [1 5 3])]
    (map #(apply hash-map (interleave [:x :fill :y] %)) cprod)))

(defn kws [kw & others]
  (fn by-kws [datum] (reduce #(conj %1 (%2 datum)) nil (conj others kw))))

(defn compare-lists
  "casts floats to ints and compares each element of the list.
  YOU ARE RESPONSIBLE FOR BEING OK WITH FLOAT->INT TRUNCATION!"
  [list1 list2]
  (let [to-int #(if (float? %) (int %) %)]
    (= (map to-int (flatten list1)) (map to-int (flatten list2)))))

(deftest test-lin-min-max-scalar
  (let [data-lists [[0 1 2 3 4 5] [5 100] [-100 100]]
        scalars (rest
                 (reductions train
                             (default-scalar :lin-min-max-zero) data-lists))
        scales (map ->scale scalars [[0 5] [0 100] [-100 100]])
        scaled-data (map
                     (fn [scale data] (map (comp int scale) data))
                     scales data-lists)]
    (is (= (nth data-lists 0) (nth scaled-data 0))
        "scaling [0 1 2 3 4 5] onto range [0 5] should give same result")
    (is (= (nth data-lists 1) (nth scaled-data 1))
        "prev. min of 0 is kept, new max of 100 is recognized.")
    (is (= (nth data-lists 2) (nth scaled-data 2))
        "new min,max: -100,100 is recognized")))

(deftest test-lin-min-max-nil
  (testing "simple nil"
    (let [data [0 1 nil]
          lin-min-max-scalar (->LinMinMaxScalar 0 0)
          trained-scalar (train lin-min-max-scalar data)
          sc (->scale trained-scalar [0 1])]
      (is (nil? (sc nil)))
      (is (float= 1 (sc 1)))
      (is (float= 0 (sc 0))))))

(deftest test-factor-ranges
  (let [data [:a :x :b]
        scale (->
               (->FactorScalar #{} (factor-comparator data))
               (train data)
               (->scale [1 4]))]
    (is (= (scale :a) [1.0 2.0]))
    (is (= (scale :x) [2.0 3.0]))
    (is (= (scale :b) [3.0 4.0]))))

(deftest test-compose-scalar
  (let [data (cartesian-product (range 4) (range 2))
        scale (-> (default-scalar :compose [(default-scalar :factor)
                                              (default-scalar :factor)]
                                    [[0 1] [0 1]])
                  (train data)
                  (->scale [0 100]))]
    (is (= (mapcat scale data) [0.0 12.5 12.5 25.0 25.0 37.5 37.5 50.0 50.0
                                62.5 62.5 75.0 75.0 87.5 87.5 100.0]))))

(deftest test-guess-scale
  (testing "guess factor scale"
    (conf/with-conf {:x [0 2]}
      (is (= [0.0 1.0]
             (doall
              (:a (guess-svg-scale :x
                                   (-> (default-scalar :factor) (train [:a :b])))))))))
  (testing "do lein clean if this fails"
    ;; If this test fails, make sure to run lein clean to blow away AOT compiled  classes.
    (is (instance? xvsy.scale.LinMinMaxScalar (->LinMinMaxScalar 0 1)))
    (is (instance? xvsy.scale.LinMinMaxScalar (xvsy.scale.LinMinMaxScalar. 0 1)))))
