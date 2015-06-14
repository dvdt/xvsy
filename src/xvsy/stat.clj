(ns xvsy.stat
  "Statistic mapping functions for various SQL databases"
  (:require [xvsy.utils :refer [apply2]])
  (:require [clojure.string]
            [korma.core]
            [korma.sql.engine :as eng]
            [korma.sql.fns :as sfns]))

(def out-of-bounds "[]")

(def db-stats
  "Holds an atom of a map of db => [stat]. Used to determine the
  supported statistics on a given database."
  (atom {:postgresql #{}
         ::goog-bq #{}}))

(defn subprotocol
  [g]
  (let [sub (or (get-in g [:db :subprotocol])
                (get-in g [:db :pool :subprotocol]))]
    (first (clojure.string/split sub #":"))))

(defmulti bind-field
  "Returns korma field with the sql-fn applied to the given column."
  (fn [g {:keys [stat]}]
    [(keyword "xvsy.stat" (subprotocol g))
                          (:name stat)]))

(defn- conj-stat
  [a k v]
  (update-in a [k] conj v))

(defmacro defstat
  "Defines a bind-field multimethod using the given body, and
  registers the stat with the db-stats atom"
  [[db stat] & body]
  `(do (defmethod bind-field ~[db stat] ~@body)
       (swap! db-stats conj-stat ~db ~stat)))

(derive ::postgresql ::sql)
(derive ::h2 ::sql)

(defstat [::sql :avg]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "AVG" (keyword (:name col)) (:opts stat)))

(defstat [::sql :count]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "COUNT" (keyword (:name col)) (:opts stat)))
(defstat [::sql :min]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MIN" (keyword (:name col)) (:opts stat)))
(defstat [::sql :max]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MAX" (keyword (:name col)) (:opts stat)))

(defstat [::sql :sum]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "SUM" (keyword (:name col)) (:opts stat)))

(defstat [::postgresql :bin]
  [g {:keys [stat col]}]
  (let [{:keys [lower upper nbins]} (:opts stat)]
    (korma.sql.engine/sql-func "WIDTH_BUCKET" (:name col)
                               (Integer/parseInt lower)
                               (Integer/parseInt upper)
                               (Integer/parseInt nbins))))
(defstat [::h2 :bin]
  [g {:keys [stat col]}]
  (let [{:keys [lower upper nbins]} (:opts stat)
        step (/ (- upper lower) nbins)
        bin-starts (range lower upper step)
        bin-stops (concat (rest  bin-starts) [upper])
        bin-ranges (map vector bin-starts bin-stops)
        sql-field (korma.sql.engine/field-str (:name col))
        case-statement
        (map (fn [[a b]]
               (format  "WHEN (%s >= %f) AND (%s < %f) THEN '[%f %f]'"
                        sql-field (float a) sql-field (float b) (float a) (float b)))
             bin-ranges)]
    ;; TODO: str bin names are messing up bin ordering! May need to parse into vec and sort?
    (clojure.string/join \newline (concat ["CASE"] case-statement
                                          [(str "ELSE " \' out-of-bounds \') "END"]))))

(defstat [::goog-bq :bin]
  [g {:keys [stat col]}]
  (let [{:keys [lower upper nbins]} (:opts stat)
        step (/ (- upper lower) nbins)
        bin-starts (range lower upper step)
        bin-stops (concat (rest  bin-starts) [upper])
        bin-ranges (map vector bin-starts bin-stops)
        case-statement
        (map (fn [[a b]]
               (format  "WHEN (%s >= %f) AND (%s < %f) THEN '[%f %f]'"
                        (:name col) (float a) (:name col) (float b) (float a) (float b)))
             bin-ranges)]
    ;; TODO: str bin names are messing up bin ordering! May need to parse into vec and sort?
    (clojure.string/join \newline (concat ["CASE"] case-statement
                                          [(str "ELSE " \' out-of-bounds \') "END"]))))

(defstat [::sql :id]
  [g {:keys [stat col]}]
  (keyword (:name col)))

(defstat [::sql :sql]
  [g {:keys [stat col]}]
  (korma.core/raw (:name col)))

(defstat [::goog-bq :avg]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "AVG" (:name col) (:opts stat)))
(defstat [::goog-bq :count]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "COUNT" (:name col) (:opts stat)))
(defstat [::goog-bq :min]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MIN" (:name col) (:opts stat)))
(defstat [::goog-bq :max]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MAX" (:name col) (:opts stat)))

(defstat [::goog-bq :sum]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "SUM" (:name col) (:opts stat)))
(defstat [::goog-bq :id]
  [g {:keys [stat col]}]
  (:name col))

;; TODO: make this conform with pg WIDTH_BUCKET
(defstat [::goog-bq :bin]
  [g {:keys [stat col]}]
  (let [{:keys [lower upper nbins]} (:opts stat)
        step (/ (- upper lower) nbins)
        bin-starts (range lower upper step)
        bin-stops (concat (rest  bin-starts) [upper])
        bin-ranges (map vector bin-starts bin-stops)
        case-statement
        (map (fn [[a b]]
               (format  "WHEN (%s >= %f) AND (%s < %f) THEN '[%f %f]'"
                        (:name col) (float a) (:name col) (float b) (float a) (float b)))
             bin-ranges)]
    ;; TODO: str bin names are messing up bin ordering! May need to parse into vec and sort?
    (clojure.string/join \newline (concat ["CASE"] case-statement
                                          [(str "ELSE " \' out-of-bounds \') "END"]))))

(defstat [nil :id]
  [g {:keys [stat col]}]
  (:name col))
(defstat [nil :avg]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "AVG" (:name col) (:opts stat)))
(defstat [nil :count]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "COUNT" (:name col) (:opts stat)))
(defstat [nil :min]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MIN" (:name col) (:opts stat)))
(defstat [nil :max]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "MAX" (:name col) (:opts stat)))
(defstat [nil :sum]
  [g {:keys [stat col]}]
  (apply korma.sql.engine/sql-func "SUM" (:name col) (:opts stat)))

(derive ::avg ::aggregator)
(derive ::count ::aggregator)
(derive ::min ::aggregator)
(derive ::max ::aggregator)
(derive ::sum ::aggregator)

(defn aggregator?
  "Determines whether the given aesthetic mapping will project the column
  onto a scalar value."
  [aes-mapping]
  (isa? (keyword "xvsy.stat" (name (get-in aes-mapping [:stat :name])))
        ::aggregator))

(defn factor?
  "Returns whether the stat-column aesthetical mapping is a categorical variable."
  [aes-mapping]
  {:post [(-> % nil? not)]}
  (let [stat-name (-> aes-mapping :stat :name)]
    (cond
      (#{:id :sql} stat-name) (-> aes-mapping :col :factor)
      (= :bin stat-name) true

      (aggregator? aes-mapping) false)))
