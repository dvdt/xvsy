(ns xvsy.aesthetics
  "Specify a plot by its aesthetic mappings, generate SQL."
  (:require [korma.sql.fns :as sfns]
            [korma.sql.engine :as eng]
            [korma.core :refer [fields group]]
            [clojure.tools.logging :as log]
            [xvsy.conf :as conf :refer [*aesthetics*]]
            [xvsy.stat :as stat]))

(defn order-aes
  "Returns the aesthetics in aes-mappings, in aesthetic order."
  [aes-mappings]
  (filter (set (for [[k v] aes-mappings
                     :when (map? v)] k)) *aesthetics*))

(defn assoc-aes-where
  [g pred-str k v]
  (eng/bind-query
   g
   (let [pred (-> pred-str symbol eng/predicates resolve)
         k (if (= (stat/subprotocol g) "goog-bq")
             (name k)
             (keyword k))]
     (korma.core/where* g (eng/pred-map (pred k v))))))

;; Google big query uses non-standard SQL that requires special modifcation.
;; Aliases may only be used in select fields, and using those
;; aliases work in group by clauses. However, aliases may not be used in
;; order-by clauses unless the same alias is used in group-by.
;; 1) BQ accepts: SELECT c1 AS x, c2 FROM mytable GROUP BY x ORDER BY x;
;; 2) BQ does not accept: SELECT x, c2 FROM mytable GROUP BY c1 AS x ORDER BY x;
;; 3) BQ does not accept: SELECT c1 AS x, c2 FROM mytable GROUP BY c1 ORDER BY x;
;; 4) BQ does not accept: SELECT c1 AS x, COUNT(c2) as y FROM mytable GROUP BY x ORDER BY x, COUNT(c2);
;; 5) BQ does accept: SELECT DEST AS x, COUNT(ORIGIN) as y FROM flights.2008 GROUP BY x ORDER BY x, y;

;; Unfortunately, 5) is non-standard SQL because GROUP BY ops occur
;; before SELECT. Therefore, we need this hacky workaround to generate
;; BQ compatible SQL.
(defn assoc-mapping
  "Add an aesthetic mapping to the given grammar of graphics plot spec."
  [g [aes aes-mapping]]
  (if (map? aes-mapping)
    (let [field (stat/bind-field g aes-mapping)
          g (korma.core/fields g [field aes])
          g (if (stat/group? aes-mapping)
              (if (= (stat/subprotocol g) "goog-bq") ; see note above.
                (korma.core/group g aes)
                (korma.core/group g field)) g)
          g (update-in g [:aesthetics] assoc aes aes-mapping)]
      g)
    ;; otherwise, this aesthetic is a constant value
    (-> g (update-in [:aesthetics] assoc aes aes-mapping)
        (update-in [:ent] (fn [ent] (korma.core/transform ent #(assoc % aes aes-mapping)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API

(defn aes
  "Binds the aesthetic mappings into a korma query object.
  A aes-mapping consists of aesthetic -> column and statistic.
  A column has the table name, whether it is factor and the datatype, as well
  as any options that impose `where` clauses in the sql query.
  A statistic is a map of consisting of the stat name and options.''"
  [g aes-mappings]
  (reduce assoc-mapping g aes-mappings))

(defn order
  "Adds order-by clauses to the query. Ordering is accomplished
  by the aesthetical order."
  [g aes-mappings]
  (let [order-by (fn assoc-order-clause [g-, aes]
                   (korma.core/order g- aes))]
    (reduce order-by g (order-aes aes-mappings))))

(defn where
  "assocs where statements with the spec. recognized pred-str are:
  = < in <= like not= > or between not >= and not-in."
  [g clauses]
  (reduce (fn [g- [p k v]] (assoc-aes-where g- p k v)) g clauses))

(defn facet
  "Assocs facetting group-by clauses to the given query. Each facet has form [aes mapping]"
  [g facets]
  (aes g (filter identity facets)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convenience functions for specifiying aesthetic mappings

(defn mapping*
  ([aes col]
   [aes {:col (if (map? col) col {:name col :factor nil})
         :stat {:name :id}}])
  ([aes col stat-name & {:as stat-opts}]
   [aes {:col (if (map? col) col {:name col :factor nil})
         :stat {:name stat-name :opts stat-opts}}]))

(defmacro x
  [v & rest]
  (cond
    (list? v) `(mapping* :x ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :x ~sym-name ~@rest))
    :else (list vector :x v)))

(defmacro fill
  [v & rest]
    (cond
    (list? v) `(mapping* :fill ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :fill ~sym-name ~@rest))
    :else (list vector :fill v)))

(defmacro color
  [v & rest]
  (cond
    (list? v) `(mapping* :color ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :color ~sym-name ~@rest))
    :else (list vector :color v)))

(defmacro size
  [v & rest]
    (cond
    (list? v) `(mapping* :size ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :size ~sym-name ~@rest))
    :else (list vector :size v)))

(defmacro facet_x
  [v & rest]
    (cond
    (list? v) `(mapping* :facet_x ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :facet_x ~sym-name ~@rest))
    :else (list vector :facet_x v)))

(defmacro facet_y
  [v & rest]
  (cond
    (list? v) `(mapping* :facet_y ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(mapping* :facet_y ~sym-name ~@rest))
    :else (list vector :facet_y v)))

(defn y*
  ([col stat-name & {:as stat-opts}]
   (if (= stat-name :id)
     (log/warn "Highly recommend that you use an aggregation function like :sum :count or :avg instead!"))
   [:y {:col (if (map? col) col {:name col :factor nil})
        :stat {:name stat-name :opts stat-opts}}]))

(defmacro y
  [v & rest]
  (cond
    (list? v) `(y* ~v ~@rest)
    (symbol? v) (let [sym-name (name v)]
                  `(y* ~sym-name ~@rest))
    :else (list vector :y v)))
