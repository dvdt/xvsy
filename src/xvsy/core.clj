(ns xvsy.core
  (:require [clojure.tools.logging :as log])
  (:require (xvsy  [geom :as geom]
                   [ggsql :as ggsql]
                   [scale :as scale]
                   [stat :as stat]
                   [aesthetics :as aesthetics]
                   [legend :as legend]
                   [utils :as utils]
                   [conf :as conf]
                   [plot :as plot]
                   [ui :as ui]))
  (:require [korma.core :as core]
            [hiccup.core :refer [html]]))

(def ^:dynamic *options* nil)

(defn x-facet-spec=y-facet-spec? [facet-spec]
  (= (get-in facet-spec [0 1])
         (get-in facet-spec [1 1])))

(defn aes-label
  "Returns svg element for x label based on the given spec"
  [{{col-name :name} :col
    {stat-name :name} :stat
    :as mapping}]
  (cond (nil? mapping) ""
        (map? mapping) (let [stat-label (if (#{:id :bin} stat-name) "" (str (name stat-name) " "))
                             label (str stat-label col-name)]
                         [:text {:class "aes-label"
                                 :font-family conf/*font-family*
                                 :font-size conf/*font-size*} label])
        :else ""))

(defn x-label [mapping]
      (update-in (aes-label mapping) [1] #(assoc % :dy "20px")))

(defn y-label
  "Returns svg element for x label based on the given spec"
  [mapping]
  (if (nil? mapping) ""
      (update-in (aes-label mapping) [1]
                 #(assoc % :transform "translate(15 0) rotate(270 0 0)"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn head-dataset
  "Returns the n rows of the dataset, the number of total rows, and
  summary statistics for each of the columns."
  [dataset & {n :n :or {n 5}}]
  (let [cols (keys (get-in @ggsql/datasets [dataset :cols]))]
    (as-> dataset $
      (@ggsql/datasets $)
      (korma.core/select* $)
      (apply korma.core/fields $ cols)
      (korma.core/limit $ 10))))

(defn col-stats
  "Returns a query for the number of non-null rows for a column. If
  column is numeric, also returns max, min and mean. For factor
  columns, returns the number of unique factors."
  [dataset [col-name {:keys [factor]}]]
  (assert (get-in @ggsql/datasets [dataset :cols col-name]))
  (let [ent (-> (@ggsql/datasets dataset)
                korma.core/select*
                (korma.core/where (not= col-name nil)))]
    (if (true? factor)
      ;; TODO: this only works for big query. Fix to be able to use postgres?
      (korma.core/fields ent [(korma.core/raw (format "COUNT(DISTINCT %s)" col-name))
                              :cnt_distinct])
      (-> ent
          (korma.core/aggregate (count col-name) :cnt)
          (korma.core/aggregate (avg col-name) :avg)
          (korma.core/aggregate (min col-name) :min)
          (korma.core/aggregate (max col-name) :max)))))

(defn factor
  "Specifies that the given column in a dataset is a factor
  (i.e. categorical) variable."
  [col-name]
  {:name col-name :factor true})

(defn non-factor*
  [col-name]
  {:name col-name :factor false})

(defmacro non-factor
  [col-sym]
  (let [col-name (name col-sym)]
    `(non-factor* ~col-name)))

(defn spec
  [dataset geom & {:keys [aes]}]
  (let [data-ent (@xvsy.ggsql/datasets dataset)
        factor-specd-aes
        (map (fn [[aes-key mapping]]
               (let [col-name (get-in mapping [:col :name])
                     col-factor? (get-in mapping [:col :factor])
                     data-factor? (get-in data-ent [:cols col-name :factor])]
                 [aes-key (assoc-in mapping [:col :factor]
                                    (if (nil? col-factor?) data-factor? col-factor?))]))
             aes)]
    {:dataset dataset :geom geom
     :aesthetics (into {} factor-specd-aes)}))

(defn ->plot-spec
  "Returns a Korma-comaptible hashmap for executing an SQL query.

  params -
  geom: an instance of xvsy.geom/Geom. Geoms represent chart types (e.g. bar, point, line)

  aes-mappings: Aesthetics mappings define how data are mapped to a
    plot. aes-mappings is a hashmap of aesthetic=>mapping, where mapping
    specifies a column and a SQL function to apply to it. For example
    the aes-mapping,
    {:x {:col {:name :my-col-name :factor false}
         :stat {:name :count}}}
    corresponds to `SELECT COUNT(my-col-name) as x`.

  where-preds: a sequence of where predicates. Each predicate takes the form [sql-func expr test].
    [\"<\" x 1] corresponds to `x < 1`

  entity: a kormasql entity

  facets: mappings (same form as for aesthetics) for plot facets.
    Should deconstruct to [facet_x_mapping facet_y_mapping]."
  [^xvsy.geom.Geom geom aes-mappings where-preds entity facet-mappings]
  (let [facet-mappings (filter identity facet-mappings)]
    (assert (reduce (fn [acc [a m]] (and a (stat/factor? m))) true facet-mappings)
            "All facet-mappings must be factors.")
    (-> (korma.core/select* entity)
        (assoc :aesthetics {})
        (aesthetics/aes aes-mappings)
        (aesthetics/order aes-mappings)
        (aesthetics/where where-preds)
        (aesthetics/facet facet-mappings))))

(defn plot-svg
  "Takes a plot specification from the http api (see xvsy.handlers),
  generates a SQL query, executes that SQL query, renders svg elements
  for the query results in accordance to that plot spec, and returns
  that hiccup SVG vector"
  [width height inline spec]
  (let [geom (or conf/*geom* (geom/default-geom (:geom spec)))
        entity (@ggsql/datasets (:dataset spec))
        facet-spec [(if-let [facet (get-in spec [:aesthetics :facet_x])] [:facet_x facet])
                    (if-let [facet (get-in spec [:aesthetics :facet_y])] [:facet_y facet])]
        aesthetics (-> spec :aesthetics (dissoc :facet_x) (dissoc :facet_y))
        ;; wheres [["in" "Dest" (map #(str \" % \") ["LAX" "IAH" "ORD" "ATL" "JFK"])]]
        wheres (or (if (map? (:where spec))
                     (ui/unreactify-wheres (:where spec))
                     (:where spec))
                   [])
        plot-spec (->plot-spec geom aesthetics wheres entity facet-spec)
        layer-data (ggsql/exec plot-spec)

        scalars (geom/guess-scalars geom (:aesthetics plot-spec))

        scalar-trainers (utils/apply-map #(partial scale/train-global-scalars %)
                                         scalars)
        facetter-trainers (if (x-facet-spec=y-facet-spec? facet-spec)
                            scale/facet-wrap
                            scale/facet-grid)
        p (plot/->plot geom scalar-trainers facetter-trainers layer-data)
        [_ _ [geom-w geom-h]] (plot/area-dims width height (:facet-scalars p))]
    (conf/with-conf  {:geom geom
                      :x (or conf/*x* [0 geom-w])
                      :y (or conf/*y* [geom-h 0])
                      :x-label (or conf/*x-label* (x-label (:x aesthetics)))
                      :y-label (or conf/*y-label* (y-label (:y aesthetics)))
                      :fill-label (or conf/*fill-label* (aes-label (:fill aesthetics)))
                      :color-label (or conf/*color-label* (aes-label (:color aesthetics)))}
      (doall (time (hiccup.core/html
                    (list "<?xml version=\"1.0\" standalone=\"no\"?>"
                          \newline
                          (if (not inline)
                            (str "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"
                    \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"
                                 \newline))
                          (plot/layout-geoms [width height] geom p))))))))

(def m-plot-svg (memoize plot-svg))

(defn qspec
  "Convenience function for concisely specifying plots. Use in
  conjunction with `plot-svg`."
  [dataset geom & {:keys [aes where]}]
  (let [dataset (name dataset)
        data-ent (@xvsy.ggsql/datasets dataset)
        _ (assert data-ent)
        factor-specd-aes
        (map (fn [[aes-key mapping]]
               (cond (map? mapping)
                     (let [col-name (name (get-in mapping [:col :name]))
                           col-factor? (get-in mapping [:col :factor])
                           data-factor? (get-in data-ent [:cols col-name :factor])]
                       [aes-key (assoc-in mapping [:col :factor]
                                          (if (nil? col-factor?) data-factor? col-factor?))])
                     :else [aes-key mapping]))
             aes)]
    {:dataset dataset :geom geom
     :aesthetics (into {} factor-specd-aes)
     :where (or where [])}))

(defn ->urlencode-spec
  "Converts a clojure plot spec into a urlencoded spec"
  [spec]
  (-> spec
      (assoc :where (ui/reactify-wheres (:where spec)))
      clojure.data.json/json-str
      ring.util.codec/url-encode))
