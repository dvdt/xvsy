(ns xvsy.schemers
  "Schemers are functions that produce a map with :data and :schema keys. They are
  used to create schemas that may adapt based on dependent input. For
  example, we can produce a schema that takes the form {:a s/Int,
  :odd? (true if :a is odd)}. Schemers may be composed. They support
  i) creating dynamic schemas based on given data ii) coercion and
  data validation cocurrent with schema generation, and iii) replacing
  schema errors with default values."
  (:require
            [clojure.tools.logging :as log]
            [clojure.algo.generic.functor :refer [fmap]]
            [clojure.walk :refer [prewalk postwalk keywordize-keys]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.macros :as smacros]
            [schema.utils :as sutils]
            [xvsy.utils :as utils]
            [xvsy.stat :as stat]
            [xvsy.ggsql :as ggsql]))

(def sql-safe-pattern
  "matches ascii alphanumeric ensconced in \"'\""
  #"\A'[\w:-]{1,50}'\z")

(defn ->sql-safe-str
  [schema]
  (if (and (isa? (type schema) java.util.regex.Pattern)
           (= (.pattern schema) (.pattern sql-safe-pattern)))
    (partial utils/ensconce \')))

(defn xvsy-coercion-matcher
  [schema]
  (or (coerce/string-coercion-matcher schema)
      (->sql-safe-str schema)))

(def where-enumerations [:a :b :c :d :e :f :g :h])

(defn geom-schema []
  (s/enum :stacked-bar :dodged-bar :histogram  :point :bin2d))

(defn dataset-schema []
  (apply s/enum (keys @ggsql/datasets)))

(def geom-aes-specs
  (let [bar {:x {:factor? true}
             :facet_x {:factor? true}
             :facet_y {:factor? true}}]
    (merge
     {:stacked-bar bar
      :dodged-bar bar
      :histogram bar}
     {:point {:facet_x {:factor? true}
              :facet_y {:factor? true}}}
     {:bin2d {:x {:factor? true}
              :y {:factor? true}
              :fill {:factor? false}
              :facet_x {:factor? true}
              :facet_y {:factor? true}}})))

(def geom-aesthetics
  (fmap #(concat % (map s/optional-key [:facet_x :facet_y]))
        {:histogram (concat (map s/required-key [:x :y]) (map s/optional-key [:fill]))
         :dodged-bar (concat (map s/required-key [:x :y]) (map s/optional-key [:fill]))
         :stacked-bar (concat (map s/required-key [:x :y]) (map s/optional-key [:fill]))
         :point (concat (map s/required-key [:x :y]) (map s/optional-key [:fill :color]))
         :bin2d (map s/required-key [:x :y :fill])}))

(defn factor-stat
  "Returns stats that will yield a factor with the given col."
  [col]
  (if (:factor col) [:id] [:bin]))

(defn numerical-stat
  "Returns stats that will yield a numerical variable with the given col."
  [col]
  (if (:factor col)
    [:count]
    [:avg :count :min :max :sum]))

(defn col-name-schemer
  [dataset]
   (apply s/enum (keys (get-in @ggsql/datasets [dataset :cols]))))

(defn where-val-schemer
  "Returns a schema for where comparisons"
  [dataset col-name pred]
  (let [col-type (:type (get-in @ggsql/datasets [dataset :cols col-name]))
        ;; whitelist user input to prevent sql injection
        col-type (if (isa? col-type s/Str)
                   sql-safe-pattern
                   col-type)]
    (if (#{"not-in" "in"} pred)
      [col-type]
      col-type)))

(defn stat-opts
  "returns a schema of options for the given stat"
  [stat]
  ;; todo: make this less hacky, i.e. more modular?
  (case stat
    (:min :max :id :count :avg :sum) nil
    :bin (s/named {:lower (s/named s/Num "lower bound")
                   :upper (s/named s/Num "upper bound")
                   :nbins (s/named s/Int "number of bins")}
                  :bin-opts)))

(defn available-stats
  "Returns the available stats for the given aesthetic in a geom. For
  example, bar plots require the x axis (col, stat) to be a factor."
  [geom-name aes col]
  (let [aes-kw (s/explicit-schema-key aes)
        factor-required? (get-in geom-aes-specs [geom-name aes-kw :factor?])]
    (case [geom-name aes-kw]
      [:histogram :y] [:count]
      [:stacked-bar :y] (if (:factor col) [:count] [:count :sum])
      (case factor-required?
        true (factor-stat col)
        false (numerical-stat col)
        nil   (concat (factor-stat col) (numerical-stat col))))))
