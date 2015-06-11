(ns xvsy.legend
  "Legends are visual elements that map plot-space to data-space. A
  legend is created for each aesthetic in a layer. Each aes-legend is
  composed of i) a scale element and ii) tick-label pairs. Each tick
  and label are also rendered. Using a legend is straightforward: the
  the caller has to merely position it. (for x and y legends, they
  ought to be positioned precisely)."
  (:require c2.ticks)
  (:require c2.svg)
  (:require [xvsy.utils :as utils])
  (:require [xvsy.scale :as scale])
  (:require [xvsy.geom :as geom])
  (:require [xvsy.conf :as conf]))

(def scientific-orders [1000000 1000 1])
(def eps 0.000000001)
(defn sci-notation
  "returns ticks in scientific notation"
  ([ticks]
     (sci-notation ticks scientific-orders))
  ([ticks log]
     (if (reduce #(and %1 %2) (map #(< (mod % (first log)) eps) ticks))
       [(map #(/ % (first log)) ticks), (first log)]
       (sci-notation ticks (rest log)))))

(defrecord Legend [iscale ticks labels])
(defmulti render-legend
  "Legends consist of a scale, tick and label elems"
  (fn [aes & rest] aes))

(defn apply-scale-range
  [[scalar range] vals]
  (-> (scale/train scalar vals) (scale/->scale range) (map vals)))

(defn scale-geoms
  [scalar-ranges geoms]
  (->> geoms utils/to-columnar
       (merge-with apply-scale-range scalar-ranges)
       utils/to-row))

(defn render-geoms
  "Returns svg elems from the given geoms. If a geom attribute is in
  the scalar-ranges map, will first apply a svg scale. Otherwise,
  attrs in geoms are untouched."
 [geom scalar-ranges geoms]
 (map (partial geom/->svg geom) (scale-geoms scalar-ranges geoms)))

(defn scale-map-vals
  [scalars ranges vals]
  (reduce (fn [scalar range] ) (map vector scalars ranges)))

(defn render-geom-ranges
  "Takes the given positioned geoms and scalars, trains scales to the
  given ranges and returns the elements"
  [geom-spec scalars geoms]
  (let [col-geoms (utils/to-columnar geoms)
        trained-scalars (utils/apply-map #(scale/train %1 %2) scalars col-geoms)
        scaled-geoms (utils/apply-map #(map  %2) trained-scalars col-geoms)
        ])

  )

(defn ->str
  "Returns a formatted string"
  [s]
  (cond
    (nil? s) "nil"
    (coll? s) (str (first s))
    :else (str s)))

(defn horizontal-labels
  [tick position]
  [:text {:class "tick-label"
           :text-anchor "middle"
           :y 5
           :x position}
   (->str tick)])

(defn vertical-labels
  [tick position]
  [:text {:transform (format "%s rotate(270,0,0)" (c2.svg/translate [position 0]))
          :class "tick-label"
          :text-anchor "end"
          :dy "0.3em"
          :x 0}
   (->str tick)])

(defn render-legend-x
  [scalar scale]
  (let [ts (scale/->ticks scalar)
        pos (map scale ts)
        pos (map #(if (coll? %) (utils/mean %) %) pos)
        svg-ticks (map #(vector :line {:class "tick" :stroke "black"
                                       :x1 %2
                                       :x2 %2
                                       :y1 0
                                       :y2 5}) ts pos)
        tick-legends (map vertical-labels ts pos)
        iscale [:line {:stroke "black" :y1 0 :y2 0 :x1 (first pos) :x2 (last pos)}]]
    (->Legend iscale svg-ticks tick-legends)))

(defn render-legend-y
  [scalar scale]
  (let [ts (scale/->ticks scalar)
        pos (map scale ts)
        pos (map #(if (coll? %) (utils/mean %) %) pos)
        svg-ticks (map #(vector :line {:class "tick" :stroke "black"
                                       :x1 0
                                       :x2 -5
                                       :y1 %2
                                       :y2 %2}) ts pos)
        tick-legends (map #(vector :text {:class "tick-label"
                                          :text-anchor "end"
                                          :y %2
                                          :dy "0.3em"
                                          :x "-0.5em"}
                                   (->str %1)) ts pos)
        ;; inverse scale maps svg cooridnates to data coords
        iscale [:line {:stroke "black" :x1 0 :x2 0 :y1 (first pos) :y2 (last pos)}]]
    (->Legend iscale svg-ticks tick-legends)))


(defmethod render-legend :x
  [aes scalar scale]
  ;; (let [ticks (scale/->ticks scalar)
  ;;       scaled-ticks (map (comp utils/->point scale) ticks)
  ;;       aes-scale (geom/->svg (geom/->Path)[{:x (first scaled-ticks) :y 0}
  ;;                       {:x (last scaled-ticks) :y 0}])

  ;;       ]aes-scale)
  (render-legend-x scalar scale))

(defmethod render-legend :y
  [aes scalar scale]
  (render-legend-y scalar scale))


;; returns a discrete color scale
(defmethod render-legend :fill
  [aes scalar scale]
  (let [fill-ticks (map scale (scale/->ticks scalar))
        geoms (map-indexed (fn [ind fill]
                             {:stroke nil :fill fill :x 0 :y [ind 1]}) fill-ticks)
        scalar-ranges {:x [(scale/default-scalar :factor) [0 conf/*fill-legend-size*]]
                       :y [(scale/default-scalar :compose)
                           [(* (+ 2  (count fill-ticks)) conf/*fill-legend-size*) 0]]}
        scaled-geoms (scale-geoms scalar-ranges geoms)
        labels (render-geoms (geom/->Text {:dy "-0.3em"}) scalar-ranges
                             (map #(assoc %1 :text (->str %2))
                                  geoms (scale/->ticks scalar)))
        iscale (render-geoms conf/*geom* scalar-ranges geoms)]
    (map->Legend {:ticks nil :labels labels :iscale iscale})))

(defmethod render-legend :color
  [aes scalar scale]
  (let [fill-ticks (map scale (scale/->ticks scalar))
        geoms (map-indexed (fn [ind fill]
                             {:color fill :fill "none" :x 0 :y [ind 1]}) fill-ticks)
        scalar-ranges {:x [(scale/default-scalar :factor) [0 conf/*color-legend-size*]]
                       :y [(scale/default-scalar :compose)
                           [(* (+ 2  (count fill-ticks)) conf/*color-legend-size*) 0]]}
        scaled-geoms (scale-geoms scalar-ranges geoms)
        labels (render-geoms (geom/->Text {:dy "-0.3em"}) scalar-ranges
                             (map #(assoc %1 :text (->str %2))
                                  geoms (scale/->ticks scalar)))
        iscale (render-geoms conf/*geom* scalar-ranges geoms)]
    (map->Legend {:ticks nil :labels labels :iscale iscale})))

(defn render-legends
  "takes a map of aes => [scalar scale] and returns a map of aes =>
  legend, where legend is a map with ticks, labels and iscale"
  [aes=>scalar-scale]
  (reduce (fn [acc, [aes [scalar scale]]]
            (assoc acc aes (render-legend aes scalar scale))) {} aes=>scalar-scale))
