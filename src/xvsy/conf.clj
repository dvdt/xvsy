(ns xvsy.conf
  "Contains dynamic vars for configuring plot layouts.
  |------------------------------------------------------------------------|
  |                        plot-padding top                                |
  |                                                                        |
  |      |-------------------------------------------------------|         |
  |      |                 facet-padding top, for facet labels   |         |
  |      |      |------------------------------------|           |         |
  | left |      |                                    |           |  right  |
  |      |      |             geoms                  |           |         |
  |      | left |                                    |  right    |         |
  |      |      |                                    |           |         |
  |      |      |                                    |           |         |
  |      |      |------------------------------------|           |         |
  |      |        facet-padding bottom, for ticks, scalebars     |         |
  |      |-------------------------------------------------------|         |
  |                        plot-padding bottom                             |
  |                         for labels, legends                            |
  |                                                                        |
  |------------------------------------------------------------------------|
  Every var in this namespace will be used to generate an api?
  "
  (:refer-clojure :exlcude [binding]))

(def ^:dynamic *aesthetics*
  "Defines an ordering of aesthetics for conducting group-by and
  order-by operations."
  [:x :color :fill :y])

(def ^:dynamic *plot-padding*
  "Should be a vec of [left right top bottom]"
  [50 125 10 50])

(def ^:dynamic *facet-padding*
  "Should be a vec of [left right top bottom]"
  [30 10 10 30])

(def ^:dynamic *color*
  "Should be bound to a coll of discrete colors, or ..."
  nil)

(def ^:dynamic *color-label*
  "Svg vector that labels the color legend"
  nil)

(def ^:dynamic *fill* nil)
(def ^:dynamic *fill-label* nil)

(def ^:dynamic *x*
  "a vector pair (i.e. range) for where geoms are scaled to."
  nil)

(def ^:dynamic *x-label* nil)

(def ^:dynamic *y*
  "Vector pair for geom height"
  nil)

(def ^:dynamic *y-label* nil)

(def ^:dynamic *facet_x*
  "range for facet drawing"
  nil)

(def ^:dynamic *facet_y* nil)
(def ^:dynamic *geom* nil)
(def ^:dynamic *fill-legend-size* 15)
(def ^:dynamic *color-legend-size* 15)
(def ^:dynamic *x-legender*
  "A function that takes an aesthetic value and it's corresponding
  svg-value, and returns a hiccup svg vector representing a legend."
  nil)
(def ^:dynamic *y-legender* nil)

(def ^:dynamic *log*)

(defn kw->var
  [kw]
  (intern 'xvsy.conf (symbol (str \* (name kw) \*))))

(defn get-conf
  "Returns the config setting for the given kw or str."
  ([v]
   (deref (kw->var v)))
  ([] (into {} (map #(vector % (get-conf %)) [:aesthetics :plot-padding
                                             :facet-padding :color
                                             :color-label :fill
                                             :fill-label :x :x-label
                                             :y :y-label :facet_x
                                             :facet_y :geom
                                             :fill-legend-size
                                             :color-legend-size]))))
(defmacro with-conf
  [bindings & body]
  (let [starred-vars (map kw->var (keys bindings))
        starred-bindings (zipmap starred-vars (vals bindings))]
    `(clojure.core/with-bindings ~starred-bindings ~@body)))
