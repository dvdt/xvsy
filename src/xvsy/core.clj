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

(defn x-facet-spec=y-facet-spec? [facet-spec]
  (= (get-in facet-spec [0 1])
         (get-in facet-spec [1 1])))

(defn aes-label
  "Returns svg element for x label based on the given spec"
  [{{col-name :name} :col
    {stat-name :name} :stat
    :as mapping}]
  (if (nil? mapping) ""
      (let [stat-label (if (#{:id :bin} stat-name) "" (str (name stat-name) " "))
            label (str stat-label col-name)]
        [:text {:class "aes-label"} label])))

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
  (log/info "plotting!")
  (let [geom (geom/default-geom (:geom spec))
        entity (@ggsql/datasets (:dataset spec))
        facet-spec [(if-let [facet (get-in spec [:aesthetics :facet_x])] [:facet_x facet])
                    (if-let [facet (get-in spec [:aesthetics :facet_y])] [:facet_y facet])]
        aesthetics (-> spec :aesthetics (dissoc :facet_x) (dissoc :facet_y))
        ;; wheres [["in" "Dest" (map #(str \" % \") ["LAX" "IAH" "ORD" "ATL" "JFK"])]]
        wheres (or (ui/unreactify-wheres (:where spec)) [])
        plot-spec (->plot-spec geom aesthetics wheres entity facet-spec)
        layer-data (ggsql/m-exec plot-spec)
        scalars (geom/guess-scalars geom (:aesthetics plot-spec))
        scalar-trainers (utils/apply-map #(partial scale/train-global-scalars %)
                                         scalars)
        facetter-trainers (if (x-facet-spec=y-facet-spec? facet-spec)
                            scale/facet-wrap
                            scale/facet-grid)
        p (plot/->plot geom scalar-trainers facetter-trainers layer-data)]
    (conf/with-conf {:plot-padding [50 125 10 50]
                     :facet-padding [30 10 10 30]
                     :geom geom}
      (let [[_ _ [geom-w geom-h]] (plot/area-dims width height (:facet-scalars p))]
        (conf/with-conf {:x [0 geom-w] :y [geom-h 0]
                         :x-label (x-label (:x aesthetics))
                         :y-label (y-label (:y aesthetics))
                         :fill-label (aes-label (:fill aesthetics))
                         :color-label (aes-label (:color aesthetics))}
          (hiccup.core/html
           (list "<?xml version=\"1.0\" standalone=\"no\"?>"
                 \newline
                 (if (not inline)
                   (str "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"
                    \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"
                        \newline))
                 (plot/layout-geoms [width height] geom p))))))))

(def m-plot-svg (memoize plot-svg))
