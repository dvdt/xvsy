(ns xvsy.test.core
  (:require [clojure.test :refer :all]
            [xvsy.core :refer :all]
            [xvsy.ggsql :as ggsql]
            [korma.db :as db]
            [korma.core :as k]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [xvsy.ui :as ui])
  (:require [clojure.math.combinatorics :refer :all]))

(db/defdb mem-db
  (db/h2 {:db "mem:xvsy_test"}))

(k/defentity triangular
  (k/database mem-db))

(def schema ["drop table if exists \"triangular\";"
             "create table \"triangular\" (\"one\" integer, \"two\" integer, \"three\" integer, \"five\" integer, \"alpha\" varchar(20));"])

(defn- reset-schema [] (dorun (map korma.core/exec-raw schema)))


(defn- triangle-row
  [r]
  (zipmap [:one :two :three :five :alpha] r))

(defn- populate-triangular
  "constructs a table with 1 * 2 * 3 * 5 * 26 / 2 = 390 rows"
  []
  (reset-schema)
  (let [triangle-vals (cartesian-product (range 1)
                                         (range 2)
                                         (range 3)
                                         (range 5)
                                         (map #(char (+ 97 %)) (range 26)))
        triangle-rows (map triangle-row triangle-vals)]
    (k/insert triangular (k/values triangle-rows))))

(def layer1 {:geom {:name :stacked-bar}
             :aesthetics {:x {:col {:name :alpha :factor true} :stat {:name :id}}
                          :y {:col {:name :five :factor false} :stat {:name :count}}}
             :entity triangular})



;;TODO: this test fails because of subtleties in group-by an aliased result.
;; (deftest test-count
;;   (populate-triangular)
;;   (let [layer-aes {:x  {:col {:name :alpha :factor true} :stat {:name :id}}
;;                    :y {:col {:name :one :factor true} :stat {:name :count}}
;;                    :fill {:col {:name :two :factor true} :stat {:name :id}}
;;                    :color {:col {:name :five :factor true} :stat {:name :id}}}
;;         res (k/exec (prepare (->LayerSpec {:name :stacked-bar} layer-aes [] triangular)))
;;         ]
;;     (is (empty?(filter #(not (= 3 (:y %))) res))
;;         "All y-vals are 3, because three is the projected column.")
;;     (is (= (* 26 1 2 5) (count res))
;;         "There are 26 * 1 * 5 results; one row for every unique value.")))

(defn- is-sorted?
  [coll]
  (let [v (vec coll)]
    (= v (sort v))))

;;TODO: this test fails because of subtleties in group-by an aliased result.
;; (deftest test-aes-ordering
;;   (populate-triangular)
;;   (with-bindings {#'xvsy.aesthetics/*aesthetics* [:a :b :c]}
;;     (let [layer-aes {:a {:col {:name :five :factor true} :stat {:name :id}}
;;                      :b {:col {:name :three :factor true} :stat {:name :id}}
;;                      :c {:col {:name :two :factor true} :stat {:name :count}}}
;;           res (k/exec (prepare (->LayerSpec nil layer-aes [] triangular)))
;;           a (vec (map :a res))
;;           ]
;;       (is (is-sorted? a)
;;           "col a is ordered at the outermost level, because it is the first in *aesthetics*")
;;       (is (true? (reduce #(and %1 %2) (map (comp is-sorted? :b second) (group-by :a res))))
;;           "every col b within each value of a is sorted"))))
;; (def simple-req
;;   {:ssl-client-cert nil, :remote-addr "0:0:0:0:0:0:0:1", :params #=(clojure.lang.PersistentArrayMap/create {"spec" "{\"a\":{\"b\":\"c\"}}", "z" "2", "f" "2"}), :route-params #=(clojure.lang.PersistentArrayMap/create {}), :headers #=(clojure.lang.PersistentArrayMap/create {"accept-encoding" "gzip, deflate, sdch", "cache-control" "max-age=0", "connection" "keep-alive", "user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.76 Safari/537.36", "accept-language" "en-US,en;q=0.8", "accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", "host" "localhost:3000", "dnt" "1", "cookie" "ring-session=5347590b-31f1-43a8-b516-1ce2929592c5"}), :server-port #=(java.lang.Integer. "3000"), :content-length nil, :form-params #=(clojure.lang.PersistentArrayMap/create {}), :query-params #=(clojure.lang.PersistentArrayMap/create {"f" "2", "z" "2", "spec" "{\"a\":{\"b\":\"c\"}}"}), :content-type nil, :character-encoding nil, :uri "/api/v1/schema", :server-name "localhost", :query-string "spec=%7B%22a%22%3A%7B%22b%22%3A%22c%22%7D%7D&z=2&f=2", :scheme :http, :request-method :get})
