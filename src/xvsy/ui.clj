(ns xvsy.ui
  (:require [clojure.core.incubator :refer [dissoc-in]]
            [clojure.tools.logging :as log]
            [clojure.algo.generic.functor :refer [fmap]]
            [clojure.walk :refer [prewalk postwalk keywordize-keys]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.macros :as smacros]
            [schema.utils :as sutils]
            [xvsy.utils :as utils]
            [xvsy.stat :as stat]
            [xvsy.ggsql :as ggsql]
            [xvsy.schemers :as schemers]))

(def REACT-OPT-KEY
  :optional)

(defn ->default
  [schema]
  (cond
    (utils/optional-entry? schema) [(:k (first schema)) (second schema)]
    (= (class schema) schema.core.EnumSchema) (first (:vs schema))
    (= (class schema) schema.core.EqSchema) (:v schema)
    (= (class schema) schema.core.NamedSchema) (:schema schema)
    (= schema s/Bool) true
    (= schema s/Num) 0
    (= schema s/Int) 0
    (= schema s/Str) ""
    (= schema s/Any) {}
    (= (class schema) java.util.regex.Pattern) ""
    (map? schema) (dissoc schema nil)
    :else schema))

(defn ->defaults
  "Returns a data structure that will pass the given schema."
  [schema]
  (clojure.walk/postwalk ->default schema))

(defn jvm->html-input
  "defines mappings of jvm types to html5 form input types"
  [klass]
  (cond
    (isa? klass s/Str) :text
    (isa? klass s/Int) :number
    (isa? klass s/Num) :number
    ;; special case implemented in react!
    (isa? klass s/Bool) :boolean))

(defn reactify
  [form]
  (cond
    (= (class form) schema.core.EnumSchema) (vec (:vs form))
    (= (class form) schema.core.NamedSchema) (into {} form)
    (= (class form) java.util.regex.Pattern) :text
    (isa? form s/Any) {}
    (isa? (class form) java.lang.Class) (jvm->html-input form)
    (isa? (class form) schema.core.Predicate) (jvm->html-input form)
    (utils/optional-entry? form)
    (do
      (assert (map? (second form)))
      [(:k (first form)) (assoc (second form) REACT-OPT-KEY true)])
    (= form s/Bool) [true false]
    :else form))

(defn reactify-schema
  "converts a schema to an json object that react understands."
  [plot-schema]
  (let [;; these schemas are manually coded by react components
        named-schemas
        (fn [form]
          (case (:name form)
            :bin-opts :bin-opts
            form))]
    (->> plot-schema
         ;; first replace all manually coded ui schemas with a name that is
         ;; will then be used to dispatch on in js.
         (prewalk named-schemas)
         ;; then automatically reactify the schema
         (postwalk reactify))))

(defn unreactify-wheres
  "should be validated! converts a reactified where spec into a sequence of xvsy where specs"
  [where-map]
  (map (fn [[k {:keys [expr1 pred expr2]}]]
         [pred expr1 expr2]) where-map))

(defn remove-optional-aes
  "removes optional aesthetics that are not present in the original data"
  [orig-data schema comp-data]
  (let [opt-aes (->> (:aesthetics schema)
                     (filter (fn [[k v]] (s/optional-key? k)) )
                     (map (comp s/explicit-schema-key first)) set)
        orig-opt-aes  (->> (:aesthetics orig-data)
                           (filter (fn [[k v]] (opt-aes k)))
                           (map (comp s/explicit-schema-key first)) set)]
    (reduce (fn [acc, aes]
              (dissoc-in acc [:aesthetics aes]))
            comp-data (clojure.set/difference opt-aes orig-opt-aes))))

(defn remove-sql-quotes
  "removes single-quote ensconced strings from where expr2"
  [spec]
  (reduce (fn desconce-expr2 [spec' e]
            (if-let [val (get-in spec [:where e :expr2])]
              (assoc-in spec' [:where e :expr2]
                        (cond
                          (and (vector? val)
                               (string? (first val))) (vec (map (partial utils/desconce \') val))
                          (string? val) (utils/desconce \' val)
                          :else val))
              spec'))
          spec schemers/where-enumerations))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; These validators return a validated data and the validation schema

(def ^:dynamic *validate* nil)

(defn json-validator
  "validates the schema on the data. attempts to coerce json
  vals. throws exception on validation errors."
  [schema data]
  (let [coerced-data
        ((coerce/coercer schema schemers/xvsy-coercion-matcher) data)]
    (if-not (:error coerced-data)
      coerced-data
      (throw (ex-info "schema invalid!" coerced-data)))))

(defn json-validator-with-defaults
  "In addition to coercing json vals, attempts to fill in missing
  errors with default values."
  [schema data]
  (let [coerced-data
        ((coerce/coercer schema schemers/xvsy-coercion-matcher) data)]
    (if-not (:error coerced-data)
      coerced-data
      (if (not (nil? (->defaults schema)))
        (->defaults schema)
        (throw (ex-info "schema invalid!" coerced-data))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; aesthetic mapping validations

(defn validate-aes-mappings
  [[aes mapping] dataset geom]
  {:pre [*validate*]}
  (let [{{col-name :name factor? :factor} :col
         {stat-name :name stat-opts :opts} :stat} mapping
         col-name-schema (schemers/col-name-schemer dataset)
         col-name (*validate* col-name-schema col-name)

         factor-schema (s/eq (get-in @ggsql/datasets [dataset :cols col-name :factor]))
         factor? (->> factor?
                      (*validate* s/Bool) ; coerce to boolean
                      (*validate* factor-schema))
         col {:name col-name :factor factor?}
         stat-name-schema (apply s/enum
                                 (schemers/available-stats geom aes col))
         stat-name (*validate* stat-name-schema stat-name)
         stat-opts-schema (schemers/stat-opts stat-name)
         stat-opts (when stat-opts-schema (*validate* stat-opts-schema stat-opts))
         ]
    {:schema {:col {:name col-name-schema :factor factor-schema}
              :stat (if stat-opts
                      {:name stat-name-schema :opts stat-opts-schema}
                      {:name stat-name-schema})}
     :data {:col col
            :stat (if stat-opts
                    {:name stat-name :opts stat-opts}
                    {:name stat-name})}}))

(defn validate-aesthetic-keys
  "ensures that aesthetic mappings are present for the geom. creates
  empty mappings for required aesthetics. doesn't check mappings:
  schema for mappings is Any"
  [aesthetics geom]
  {:pre [(keyword? geom) *validate*]}
  (let [available-aesthetics (geom schemers/geom-aesthetics)
        aesthetics-schema (into {} (map #(vector % s/Any) available-aesthetics))
        aesthetics (merge (into {} (for [aes available-aesthetics
                                         :when (not (s/optional-key? aes))]
                                     [aes {}]))
                          aesthetics)
        aesthetics (*validate* aesthetics-schema aesthetics)]
    {:data aesthetics :schema aesthetics-schema}))

(defn validate-aesthetics
  [aesthetics dataset geom]
  {:pre [(keyword? geom)]}
  (let [{aesthetics :data schema :schema :as result} (validate-aesthetic-keys aesthetics geom)]
    (reduce (fn [acc, [aes _]]
              (if-let [aes-mapping ((s/explicit-schema-key aes) aesthetics)]
                (let [{aes-mapping :data mapping-schema :schema}
                      (validate-aes-mappings [aes aes-mapping] dataset geom)]
                  (-> acc
                      (assoc-in [:data (s/explicit-schema-key aes)] aes-mapping)
                      (assoc-in [:schema aes] mapping-schema)))
                acc)) result schema)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; where validations

(defn validate-where-statement
  [{:keys [expr1 pred expr2]} dataset]
  {:pre [*validate*]}
  (let [expr1-schema (apply s/enum (keys (get-in @ggsql/datasets [dataset :cols])))
        expr1 (*validate* expr1-schema expr1)
        pred-schema (s/enum ">" ">=" "<" "<=" "not=" "=" "not-in" "in")
        pred (*validate* pred-schema pred)
        expr2-schema (schemers/where-val-schemer dataset expr1 pred)
        expr2 (*validate* expr2-schema expr2)]
    {:data (utils/remap expr1 pred expr2)
     :schema {:expr1 expr1-schema :expr2 expr2-schema :pred pred-schema}}))

(defn validate-where-keys
  [where-map]
  (let [available-wheres (take (inc (count where-map)) schemers/where-enumerations)
        where-schema (into {} (map #(vector (if (% where-map) % (s/optional-key %))
                                            s/Any) available-wheres))
        where-map (merge (into {} (for [where-key available-wheres :when (where-key where-map)]
                                    [where-key {}]))
                         where-map)
        where-map (*validate* where-schema where-map)]
    {:data where-map :schema where-schema}))

(defn validate-where
  [where-map dataset]
  {:pre [*validate*]}
  (let [{where-map :data where-schema :schema :as result} (validate-where-keys where-map)]
    (reduce (fn [acc, [where-key where-statement]]
              (if where-statement
                (let [{where :data where-schema :schema} (validate-where-statement where-statement dataset)]
                  (-> acc (assoc-in [:data (s/explicit-schema-key where-key)] where)
                      (assoc-in [:schema where-key] where-schema)))
                acc))
            result
            where-map)))

(defn validate-spec
  [data]
  {:pre [*validate*]}
  (let [{:keys [dataset geom aesthetics where]} (-> data utils/remove-nils keywordize-keys)
        dataset-schema (schemers/dataset-schema)
        dataset (*validate* dataset-schema dataset)
        geom-schema (schemers/geom-schema)
        geom (*validate* geom-schema geom)

        {aesthetics :data aesthetics-schema :schema}
        (validate-aesthetics aesthetics dataset geom)

        {validated-where :data where-schema :schema}
        (validate-where where dataset)
        where (if where validated-where nil)

        schema {:dataset dataset-schema
                :geom geom-schema
                :aesthetics aesthetics-schema
                (s/optional-key :where) where-schema}]
    {:data (->> (utils/remap dataset geom aesthetics where)
                utils/remove-nils)
     :schema schema}))
