(ns xvsy.test.ui
  (:require [clojure.test :refer :all]
            [xvsy.ui :refer :all]
            [xvsy.schemers :as schemers]
            [clojure.walk :as walk]
            [clojure.data.json :as json]
            [xvsy.ggsql :as ggsql]
            [schema.core :as s]
            [schema.coerce :as c]
            [clojure.data :refer [diff]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validator testing

(defmacro validator-testing
  [string & body]
  `(testing ~string
     (with-redefs [ggsql/datasets
                   (atom {"data" {:cols {"num-col" {:factor false :type s/Num}
                                         "factor-col" {:factor true :type s/Str}}}})]
       (binding [*validate* json-validator]
         ~@body))))

(defmacro default-testing
  [string & body]
  `(testing ~string
     (with-redefs [ggsql/datasets
                   (atom {"data" {:cols {"num-col" {:factor false :type s/Num}
                                         "factor-col" {:factor true :type s/Str}}}})]
       (binding [*validate* json-validator-with-defaults]
         ~@body))))

(deftest test-validate-aes-mappings
  (validator-testing
   "simple"
   (let [x-mapping {:col {:name "factor-col" :factor true}
                    :stat {:name :id}}
         y-mapping {:col {:name "num-col" :factor false}
                    :stat {:name :bin :opts {:lower 0.0 :upper 1.1 :nbins 3}}}]
     (is (= x-mapping
            (:data (validate-aes-mappings [:x x-mapping] "data" :point))))
     (is (= y-mapping
            (:data (validate-aes-mappings [:y y-mapping] "data" :point))))
     (validate-aes-mappings [:y y-mapping] "data" :point)))
  (validator-testing
   "optional aesthetics are specified in schema"
   (is (validate-aes-mappings [(s/optional-key :facet_x)
                               {:col {:name "factor-col" :factor true}
                                :stat {:name :id}}] "data" :point)))
  (default-testing
    "optional aesthetics are specified in schema"
    (is (validate-aes-mappings [(s/optional-key :facet_x)
                               {}] "data" :point)))
  (validator-testing
   "json coercions"
   (let [x-mapping {:col {:name "factor-col" :factor "True"}
                    :stat {:name "id"}}
         y-mapping {:col {:name "num-col" :factor "FALSE"}
                    :stat {:name "bin" :opts {:lower "0.0" :upper "1.1" :nbins "3"}}}]
     (is (= {:col {:name "factor-col" :factor true}
             :stat {:name :id}}
            (:data (validate-aes-mappings [:x x-mapping] "data" :point)))
         "x-mapping json strings are coerced")
     (is (= {:col {:name "num-col" :factor false}
             :stat {:name :bin :opts {:lower 0.0 :upper 1.1 :nbins 3}}}
            (:data (validate-aes-mappings [:y y-mapping] "data" :point)))
         "y mapping json strings are coerced")))
  (validator-testing
   "incomplete schema throws exception"
   (let [x-mapping {:col {:name "non-existent-col" :factor "True"}
                    :stat {:name "id"}}]
     (is (thrown? Exception (validate-aes-mappings [:x x-mapping] "data" :point))))))

(deftest test-validate-aesthetic-keys
  (validator-testing
   "simple"
   (is (= :my-val (get-in
                   (validate-aesthetic-keys {:x {:my-key :my-val} :y {} :facet_x {}} :point)
                   [:data :x :my-key])))
   (is (thrown? Exception (validate-aesthetic-keys {:bad-aes-key {} :x {} :y {} :facet_x {}} :point)))
   (is (= {} (get-in (validate-aesthetic-keys
                      {:x {} :facet_x {}} :point) [:data :y])))
   (is (= {} (get-in (validate-aesthetic-keys
                      nil :point) [:data :y])))))

(deftest test-validate-aesthetic
  (validator-testing
   "simple"
   (let [{aesthetics :data schema :schema}
         (validate-aesthetics {:x {:col {:name "num-col" :factor "FALSE"}
                                   :stat {:name "bin" :opts {:lower "0.0" :upper "1.1" :nbins "3"}}}
                               :y {:col {:name "factor-col" :factor true}
                                   :stat {:name :id}}} "data" :point)]
     (is (= {:col {:name "num-col" :factor false}
             :stat {:name :bin :opts {:lower 0.0 :upper 1.1 :nbins 3}}}
            (:x aesthetics))
         "json coercion of string data")
     (is (and (schema (s/optional-key :fill))
              (schema (s/optional-key :color)))
         "optional aesthetics keyed in schema")))
  (default-testing
    "fills in missing required aesthetics"
    (let [{aesthetics :data schema :schema}
          (validate-aesthetics {:x {:col {:name "num-col" :factor "FALSE"}
                                    :stat {:name "bin" :opts
                                           {:lower "0.0" :upper "1.1" :nbins "3"}}}}
                               "data" :point)]
      (is (get-in aesthetics [:y :col :name]))
      (is (= (s/enum "num-col" "factor-col")
             (get-in schema [:y :col :name])))))
  (default-testing
    "fills in optional schema if value is non nil"
    (let [{aesthetics :data schema :schema}
          (validate-aesthetics {:facet_x {}}
                               "data" :point)]

      (is (get-in aesthetics [:facet_x :col :name]))
      (is (get-in schema [(s/optional-key :facet_x) :col :name]))
      (is (nil? (get-in aesthetics [:fill])))
      (is (= (s/enum "num-col" "factor-col")
             (get-in schema [(s/optional-key :facet_x) :col :name])))
      schema))
  (default-testing
    "simple"
    (is (validate-aesthetics {} "data" :point))))

(deftest test-validate-wheres
  (validator-testing
    "simple"
    (validate-where-statement  {:expr1 "factor-col" :expr2 "asdf" :pred "="} "data")
    (validate-where-statement  {:expr1 "num-col" :expr2 "10.0" :pred "="} "data"))
  (validator-testing
   "where-keys"
   (let [{m :data sc :schema} (validate-where-keys {})]
     (is (empty? m))
     (is (get sc (s/optional-key :a)))))
  (validator-testing
   "empty wheremap"
   (let [{m :data sc :schema} (validate-where nil "data")]
     sc
     ))
  (default-testing
    "simple defaults"
    (-> (validate-where-statement {} "data")
        (get-in [:data]))))


(deftest schema-generation
  (validator-testing
   "validate schema for complete data"
   (let [data
         {:geom :stacked-bar :dataset "data"
          :aesthetics {:x {:col {:name "num-col" :factor false}
                           :stat {:name :bin :opts {:lower 0 :upper 0 :nbins 10}}
                           }
                       :y {:col {:name "factor-col" :factor true}
                           :stat {:name :count}}}}]
     (is (= data (:data (validate-spec data))))))
  (validator-testing
   "generate schema from keys that are strings"
   (let [str-key-data
         {"geom" "stacked-bar" :dataset "data"
          "aesthetics" {:x {:col {:name "num-col" :factor false}
                            :stat {:name :bin :opts {:lower 0 :upper 10 :nbins 10}}}
                        :y {:col {:name "factor-col" :factor true}
                            :stat {:name :count}}}}]
     (is (= :bin (get-in (validate-spec str-key-data)
                         [:data :aesthetics :x :stat :name])))
     (is (= 10 (get-in (validate-spec str-key-data)
                       [:data :aesthetics :x :stat :opts :nbins])))))
  (validator-testing
   "generate schema when data are strings, instead of kws"
   (let [str-data
         {:geom :stacked-bar :dataset "data"
          :aesthetics {:x {:col {:name "num-col" :factor false}
                           :stat {:name "bin" :opts {:lower 0 :upper 10 :nbins "10"}}}
                       :y {:col {:name "factor-col" :factor true}
                           :stat {:name "count"}}}}]
     (is (= :bin (get-in (validate-spec str-data)
                         [:data :aesthetics :x :stat :name])))
     (is (= 10 (get-in (validate-spec str-data)
                       [:data :aesthetics :x :stat :opts :nbins])))
     (is (= :count (get-in (validate-spec str-data)
                           [:data :aesthetics :y :stat :name])))))
  (default-testing
    "completes schema for optional aesthetics that are keyed"
    (let [data
          {:aesthetics {:facet_x {}}}]
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x]))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x :col]))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x :col :name]))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x :col :factor]))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x :stat]))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_x :stat :name]))
      (is (get-in (-> data validate-spec :data)
                  [:aesthetics :facet_x :stat :name]))
      (is (nil? (get-in (-> data validate-spec :data)
                        [:aesthetics :facet_y])))
      (is (get-in (-> data validate-spec :schema reactify-schema)
                  [:aesthetics :facet_y])))))
