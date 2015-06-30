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
  Every var in this namespace will be used to generate an api?")

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

(def ^:dynamic *size*
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

(def ^:dynamic *font-family* "monospace")
(def ^:dynamic *font-size* "12")


(defn get-conf
  "Returns the config setting for the given kw or str."
  ([v]
   (get get-conf v))
  ([]
   {:aesthetics *aesthetics*
    :plot-padding *plot-padding*
    :facet-padding *facet-padding*
    :color *color*
    :color-label *color-label*
    :color-legend-size *color-legend-size*
    :fill *fill*
    :fill-label *fill-label*
    :fill-legend-size *fill-legend-size*
    :x *x*
    :x-label *x-label*
    :x-legender *x-legender*
    :y *y*
    :y-label *y-label*
    :y-legender *y-legender*
    :facet_x *facet_x*
    :facet_y *facet_y*
    :geom *geom*
    }))

#?(:clj
    (defn kw->var
           [kw]
           (intern 'xvsy.conf (symbol (str \* (name kw) \*))))
    )
#?(:clj
   (defmacro with-conf
     [bindings & body]
     (let [starred-vars (map kw->var (keys bindings))
           starred-bindings (zipmap starred-vars (vals bindings))]
       `(clojure.core/with-bindings ~starred-bindings ~@body))))

