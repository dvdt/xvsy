(ns xvsy.plot
  "This namespace is responsible for generating svg plots from sql data."
  (:require
   [clojure.algo.generic.functor :refer [fmap]]
   [clojure.tools.logging :as log]
   [schema.core :as s]
   [xvsy.utils :as utils]
   [xvsy.conf :as conf]
   [xvsy.scale :as scale]
   [xvsy.geom :as geom]
   [xvsy.legend :as legend]
   [c2.svg :refer [translate transform-to-center]]))

(defn area-dims
  "Given the overall plot width and height and facet scalars, returns
  the facet-area dimensions, which is the width and height of the
  space that all facets fit into; facet dimensions, for a single
  facet; and the geom dimensions, which is where the actual plot geom
  elements go. Example below has a single facet. Global legends can go
  in (a), and per-facet legends go in (b).

                    width
  |-------------------------------------------------|
  |                                                 |
  |            facet-area-width                     |
  |    |------------------------------|             |
  |    |                              |             | height
  |    |        facet-width           |             |
  |    |    |---------------|   (b)   |     (a)     |
  |    |                              |             |
  |    |       geom-width             |             |
  |    |       |--------|             |             |
  |    |                              |             |
  |    |                              |             |
  |    |------------------------------|             |
  |                                                 |
  |                (a)                              |
  |-------------------------------------------------|"
  [width height [facet-x-scalar facet-y-scalar]]
  (let [[plot-padding-left plot-padding-right
         plot-padding-top plot-padding-bottom] conf/*plot-padding*
         [facet-padding-left facet-padding-right
          facet-padding-top facet-padding-bottom] conf/*facet-padding*
          [facet-area-w facet-area-h] [(- width plot-padding-left plot-padding-right)
                                     (- height plot-padding-top plot-padding-bottom)]
          facet-w (/ facet-area-w (count (scale/->ticks facet-x-scalar)))
          facet-h (/ facet-area-h (count (scale/->ticks facet-y-scalar)))
        [geom-w geom-h] [(- facet-w facet-padding-left facet-padding-right)
                         (- facet-h facet-padding-top facet-padding-bottom)]]
    [[facet-area-w facet-area-h] [facet-w facet-h] [geom-w geom-h]]))

(defn plot-width
  [width]
  (- width (nth conf/*plot-padding* 0) (nth conf/*plot-padding* 1)))

(defn plot-height
  [height]
  (- height (nth conf/*plot-padding* 2) (nth conf/*plot-padding* 3)))

(defn facet-width
  "assumes facets are of same width. returns the width in pixels of a
  facet, given with width of the entire plot."
  [width facet-scalar]
  (let [facet-full-length (second ((scale/->scale facet-scalar [0 (plot-width width)])
                                         (first (scale/->ticks facet-scalar))))]
    (- facet-full-length (nth conf/*facet-padding* 0) (nth conf/*facet-padding* 1))))

(defn facet-height
  "assumes facets are of same width"
  [height facet-scalar]
  (let [facet-full-length (second ((scale/->scale facet-scalar [0 (plot-height height)])
                                   (first (scale/->ticks facet-scalar))))]
    (- facet-full-length (nth conf/*facet-padding* 2) (nth conf/*facet-padding* 3))))

(defn geom-height
  "assumes facets are of same width"
  [facet-scalar facet-area-length]
  (let [facet-length  (/ facet-area-length (count (scale/->ticks facet-scalar)))]
    (- facet-length (nth conf/*facet-padding* 2) (nth conf/*facet-padding* 3))))

(defn make-discrete-scale
  [scalar range-vals]
  (if (nil? scalar) nil
      (let [t (scale/->ticks scalar)
            s (scale/->scale scalar [0 (count t)])
            s (if (coll? (s (first t)))
                (comp first s)
                (comp #(if (< % (count range-vals)) % (- (count range-vals) 1))
                      s))
            ]
        (fn [v] (nth range-vals (mod (s v) (count range-vals)))))))

(defn make-continuous-scale
  [scalar svg-fn]
  (comp svg-fn (scale/->scale scalar [0 1])))

(defn- gen-scales
  "Returns scales from the scalars and dynamically bound ranges"
  [scalar-map]
  (-> scalar-map
      (update-in [:color] #(make-discrete-scale % conf/*color*))
      (update-in [:fill] #(make-discrete-scale % conf/*fill*))))

(defn- scale-geoms
  [scale-map geom-data]
  (map (fn scale-geom
         [g]
         (utils/apply-map #(%1 %2) scale-map g))
       geom-data))

(defn wrap-facet
  "returns translated [:g] elements for each facet-val"
  [[x-scale y-scale] [x-val y-val]]
  [:g {:class "facet" :transform
       (translate [(apply min (x-scale x-val))
                   (apply min (y-scale y-val))])}])

(defn last-row?
  [[x-scalar y-scalar] [x y]]
  (-> y ((scale/->scale y-scalar [0 10000])) first int (= 0)))

(defn first-col?
  [[x-scalar y-scalar] [x y]]
  (-> x ((scale/->scale x-scalar [0 10000]))  first int (= 0)))

;; TODO: The impl is super ugly right now, but I think the API is right.

(defn wrap-geoms
  "Draws legends around a facet."
  [{:keys [facet-scalars facet-scales geom-scalars geom-scales]
    :as plot} [width height] [facet rendered-geoms]]
  (let [[facet-w facet-h] [(facet-width width (first facet-scalars))
                           (facet-height height (second facet-scalars))]
        x-legend (legend/render-legend :x (get-in geom-scalars [facet :x])
                                       (get-in geom-scales [facet :x]))
        y-legend (legend/render-legend :y (get-in geom-scalars [facet :y])
                                (get-in geom-scales [facet :y]))
        aes-legends (map #(legend/render-legend % (get-in geom-scalars [facet %])
                                                (get-in geom-scales [facet %]))
                         (keys geom-scales))]
    [:g {:class "geoms"}
     [:g {:transform (translate [0 facet-h])}
      (:ticks x-legend)
      (if (last-row? facet-scalars facet)
        [:g {:transform (translate [0 15])}
         (:labels x-legend)])]

     [:g {:transform (translate [0 0])}
      (:ticks y-legend)
      (if (first-col? facet-scalars facet)
        (:labels y-legend))]

     [:rect {:x 0 :y 0 :width facet-w :height
             facet-h :fill :none :stroke "grey"
             :class "facet-outline"}]
     rendered-geoms]))


(defn bind-facetting-scales
  "Given a width and height, generates :facet-scales
  based on the bound conf. Facet scales is a vec: [facet-x-scale facet-y-scale]"
  [{:keys [facet-scalars geom-scalars facetted-geom-data]
    :as plot} [width height]]
  (assoc plot :facet-scales
         [(scale/->scale (first facet-scalars) [0 (plot-width width)])
          (scale/->scale (second facet-scalars) [(plot-height height) 0])]))

(defn bind-geom-scales
  "Returns a plot with the key :geom-scales set as a map of:
  [facet-x facet-y] => (aes => (data-coords -> svg-coords)).
  args:
  facet-scalars is a vec: [facet-x-scalar facet-y-scalar]
  geom-scalars is a map: [facet-x facet-y] => (aes => scalar)
  facetted-geom-data is a map: [facet-x facet-y] => [aes => data]"
  [{:keys [facet-scalars geom-scalars facetted-geom-data]
    :as plot}]
  (assoc plot :geom-scales
         (fmap scale/guess-svg-scales geom-scalars)))

(defn facet-by
  "returns a hashmap of facet -> data"
  [[[x-facet x-mapping] [y-facet y-mapping]] data]
  (group-by (juxt x-facet y-facet) data))

(defn ->plot
  "Returns trained scales and positioned geom hashmaps.
  scalar-trainer is hashmap of aes-kw => ([data] -> scalar)
  facetter-trainers is ([facet] -> [facet-x-scalar facet-y-scalar])"
  [geom scalar-trainers facetter-trainers sql-data]
  (let [facetted-col-data (->> sql-data
                               (facet-by [[:facet_x nil] [:facet_y nil]])
                               (fmap (partial geom/adj-position geom))
                               (fmap utils/to-columnar))
        trained-geom-scalars (utils/apply-map #(%1 %2) scalar-trainers
                                              (utils/to-columnar (vals facetted-col-data)))
        trained-geom-scalars (zipmap (keys facetted-col-data) (utils/to-row trained-geom-scalars))
        trained-facet-scalars (facetter-trainers (keys facetted-col-data))]
    {:facet-scalars trained-facet-scalars
     :geom-scalars trained-geom-scalars
     :facetted-geom-data (fmap utils/to-row facetted-col-data)}))

(defn render-facet-geoms
  "Returns facet=>[svg-geom-elems]"
  [facetted-geom-scales facetted-geom-data]
  (as-> facetted-geom-data $
    ;; Transform  geoms to svg-coords
    (utils/apply-map scale-geoms facetted-geom-scales $)
    ;; Transform svg-coord geom maps to svg elements
    (fmap
     (fn render-geoms [scaled-geoms]
       (map (partial geom/->svg conf/*geom*) scaled-geoms)) $)))

(defn render-facet-legends
  "Returns facet=>(aes=>Legend)"
  [facet=>aes-scalars facet=>aes-scales]
  (let [facet=>aes-scalar-scales  (utils/apply-map
                                        #(merge-with vector %1 %2)
                                        facet=>aes-scalars facet=>aes-scales)]
    (fmap legend/render-legends facet=>aes-scalar-scales)))

(defn draw-xy-iscales-ticks
  [legends]
  (list [:g {:class "x-iscale"
             :transform (translate [0 (first conf/*y*)])}
         (get-in legends [:x :iscale])
         (get-in legends [:x :ticks])]
        [:g {:class "y-iscale"
             :transform (translate [(first conf/*x*) 0])}
         (get-in legends [:y :iscale])
         (get-in legends [:y :ticks])]))

(defn draw-facet-xy-labels
  "Draws labels if facet is first column or last row"
  [facet-scalars {x-legend :x y-legend :y} facet]
  [:g [:g {:transform (translate [0 (first conf/*y*)])}
       (if (last-row? facet-scalars facet)
         [:g {:transform (translate [0 10])}
          (:labels x-legend)])]
   [:g {:transform (translate [(first conf/*x*) 0])}
    (if (first-col? facet-scalars facet)
      (:labels y-legend))]])

(defn facet-labeller
  [facet-x facet-y]
  (let [attrs {:transform (translate [0 0])
               :text-anchor "start"}]
    (cond
      (and (nil? facet-x) (nil? facet-y)) nil
      (and facet-x facet-y) [:text attrs
                             (format "(%s, %s)" facet-x facet-y)]
      (nil? facet-x) [:text attrs (str facet-y)]
      (nil? facet-y) [:text attrs (str facet-x)])))

(defn layout-geoms
  "Returns a svg hiccup vector plot. Takes width, height of the svg elem;
  trained facet and layer scalars; and geom positioned data.

  expects conf/*[aes]* and conf/*[aes]-label* vars to be bound

  args
  facet-scalars: scalar for positioning facets
  geom-scalars: a map of facet => aes-scalars,
                   where aes-scalars is itself a map of aes => scalar
  facetted-geom-data: a map of facet => positioned data"

  [[width height] geom {:keys [facet-scalars geom-scalars
                               facetted-geom-data] :as plot}]
  {:pre [(not (nil? conf/*x*)) (not (nil? conf/*y*))]}
  (let [[[facet-area-w facet-area-h] [facet-w facet-h] [geom-w geom-h]]
        (area-dims width height facet-scalars)

        scaled-plot (-> plot (bind-facetting-scales [width height])
                        bind-geom-scales)
        facet=>svg-wrapper (fmap (partial wrap-facet (:facet-scales scaled-plot))
                                 (zipmap (keys facetted-geom-data) (keys facetted-geom-data)))
        facet=>svg-geoms (render-facet-geoms (:geom-scales scaled-plot) facetted-geom-data)
        facet=>Legends (render-facet-legends geom-scalars (:geom-scales scaled-plot))
        facet=>svg-elem
        (as-> facet=>svg-wrapper $
          ;; translate facet-padding
          (fmap (fn [_] (vector :g {:transform (translate [(nth conf/*facet-padding* 0)
                                                           (nth conf/*facet-padding* 2)])}))
                $)
          ;; Draw xy scale bars in each facet
          (utils/apply-map #(conj %1 (draw-xy-iscales-ticks %2)) $ facet=>Legends)
          ;; Draw text labels for xy legends
          (utils/apply-key-map
           (fn [facet facet-elem facet-legend]
             (conj facet-elem
                   (draw-facet-xy-labels facet-scalars facet-legend facet)))
           $ facet=>Legends)
          ;; Draw geoms in each facet
          (utils/apply-map conj $ facet=>svg-geoms)
          ;; wrap facet
          (utils/apply-map (fn [wrapper geoms] (conj wrapper geoms)) facet=>svg-wrapper $)
          ;; draw facet-x and facet-y label
          (utils/apply-key-map (fn [[facet-x facet-y] wrapper]
                                 (conj wrapper (facet-labeller facet-x facet-y))) $))]
    [:svg {:width width :height height :xmlns "http://www.w3.org/2000/svg"}
     [:g {:transform (translate [(nth conf/*plot-padding* 0)
                                 (nth conf/*plot-padding* 2)])}
      (vals facet=>svg-elem)]
     ;; draw global aesthetic legends (except for x, y)
     [:g {:class "legends" :transform (translate [(- width (nth conf/*plot-padding* 1))
                                                  (nth conf/*plot-padding* 2)])}
      (let [[_ {:keys [color fill]}] (first facet=>Legends)]
        (list
         (if color [:g {:class "color-legend"}
                    conf/*color-label*
                    (:iscale color)
                    [:g {:transform (translate [conf/*color-legend-size*
                                                conf/*color-legend-size*])}
                     (:ticks color)
                     [:g {:transform (translate [3 0])} (:labels color)]]])
         (if fill [:g {:class "fill-legend" :transform (translate [conf/*color-legend-size* 0])}
                   conf/*fill-label*
                   (:iscale fill)
                   [:g {:transform (translate [conf/*fill-legend-size*
                                               (/ conf/*fill-legend-size* 2)])}
                    (:ticks fill)
                    [:g {:transform (translate [3 0])}
                     (:labels fill)]]])))]
     ;; draw x and y labels
     [:g {:transform (translate [(/ width 2) (- height (nth conf/*plot-padding* 3))])}
      conf/*x-label*]
     [:g {:transform (translate [0 (/ height 2)])}
      conf/*y-label*]]))
