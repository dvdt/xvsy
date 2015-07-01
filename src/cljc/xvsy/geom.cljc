(ns xvsy.geom
  (:require
    [xvsy.scale :as scale]
    [xvsy.utils :as utils]
    [xvsy.conf :as conf]))

;; These aesthetics, when are factor, are specified by css classes.
(def css-aesthetics #{:color :fill
                      :alpha})

(defn- css-class-name
  "Returns a css class name from a factor.
  [{:x :x1} [:x {:column {:name :x}}]] => \"x-x1\""
  [datum [aes aes-mapping]]
  (let [col-name (if (coll? (:column aes-mapping))
                   (-> :column aes-mapping first name)
                   (-> :column aes-mapping name))]
    (str col-name \- (-> aes datum))))

(defn format-float [x]
  #?(:clj (format "%.3fpx" x))
  #?(:cljs (str (.toString x) "px")))

(defn css-classes
  "Returns a map of css defs for the given datum"
  [aes-mappings scales norm-datum]
  (let [factor-aesthetics (filter #(and (-> % first css-aesthetics) (-> % second :factor)) aes-mappings)
        classes (map (partial css-class-name norm-datum) factor-aesthetics)
        class-vals (map (fn [[aes _]] ((aes scales) (aes norm-datum))) factor-aesthetics)]
    (apply hash-map (interleave classes class-vals))))

;; aesthetic vals -> attribute pair functions
(defn x-interval [[x1 x2]]
  "X Interval -> :x, :width attributes"
  (let [x (min x1 x2)
        width (utils/abs (- x1 x2))]
    [:x (format-float x) :width (format-float width)]))

(defn y-interval [[y1 y2]]
  (let [y (min y1 y2)
        height (utils/abs (- y1 y2))]
    [:y (format-float y) :height (format-float height)]))

(defn x-point [x] [:cx x])
(defn y-point [y] [:cy y])
(defn fill [rgb] [:fill rgb])
(defn color [rgb] [:stroke rgb])

(defn bind-attrs
  "Returns attr-val pairs for each aesthetic, as a map"
  [aes-svg aes-datum]
  (let [aess (keys aes-svg)
        attr-val-pairs (reduce (fn [attr-vals, aes]
                                 (if-let [aes-val (aes aes-datum)]
                                   (conj attr-vals ((aes aes-svg) aes-val))
                                   attr-vals)) [] aess)]
    (->> attr-val-pairs flatten (apply hash-map))))


(defn bind-css
  [aes-mappings scaled-datum raw-datum]
  (reduce (fn [acc, [a m]]
            (if (:factor m)
              (update-in acc [:class] str " "
                (css-class-name raw-datum [a m])) acc)) scaled-datum aes-mappings))
(defn dodge
  "Returns data dodged on given aesthetics. For example, dodging by [:x :c] on:
  [{:x :a :c :1} {:x :a :c :2}] => [{:x [:a :1] :c :1} {:x [:a :2] :c :2}].
  params:
  layer-data: data returned from sql query
  dodged-aesthetics: vec of aesthetics to dodge on, in order. first aesthetic is
  updated."
  [layer-data dodged-aesthetics]
  {:pre [(not (empty? dodged-aesthetics))]}
  (let [x-dodged #(map % dodged-aesthetics)
        aes (first dodged-aesthetics)]
    (map #(assoc % aes (x-dodged %)) layer-data)))

(defn- stack-within-group
  [stack-aes grouped-data]
  (let [vals (map stack-aes grouped-data)
        new-vals (reductions + vals)
        new-val-intervals (map vector (conj new-vals 0) new-vals)]
    (map #(assoc %1 stack-aes %2) grouped-data new-val-intervals)))

(defn stack
  "Stacks all data that have the same \"group\" aes on top of each
  other using the \"by\" aes. nil values are ignored in the stacking
  and returned unchanged."
  [dodged-data group-aes by-aes]
  (if (nil? by-aes) dodged-data
                    (let [stack-grouped-rows
                          (fn [rows]
                            (let [nil-rows (group-by (comp nil? by-aes) rows)]
                              (concat (stack-within-group by-aes (nil-rows false))
                                (nil-rows true))))
                          groups (vals (group-by group-aes dodged-data))]
                      (mapcat stack-grouped-rows groups))))

(defn position-bars
  "Dodges and stacks bars.  Bars are aligned along an axis (unless
  stacked). A typical bar plot has all the bars aligned at
  x=0. Direction of alignment (i.e. left vs right, top vs down) is
  determined by the scale. Stack and dodge is alignment aware.
  nil values are not included."
  [geom layer-data]
  (let [{:keys [direction aes-positions] :or
         {direction :x
          aes-positions []}} geom
        dodged-aes (filter
                     (fn [[aes p]] (= :dodge p)) aes-positions)
        ;; the first dodged aesthetic is always the "direction"
        dodged-aes (map first (conj dodged-aes [direction :dodge]))
        stacked-aes (if (= :x direction) :y :x)
        pos-data (-> layer-data (dodge dodged-aes)
                   (stack direction stacked-aes))]
    pos-data))

(defn jitter-points
  "TODO"
  [layer layer-data]
  layer-data)

(defprotocol Geom
  (->svg [this elem-map] "Returns hiccup svg vector representation")
  (adj-position [this sql-data]
    "Performs geom specific adjustments to the data retrieved from a sql
query")
  (guess-scalars [this aes-mappings]))

(defn- required
  [x]
  (complement (nil? x)))

(defn optional-aesthetic
  [mapping scalar]
  (cond
    (map? mapping) scalar
    (nil? mapping) scalar
    :else (scale/->ConstantScalar mapping)))

;; aes-positions
(defrecord Bar
  [direction aes-positions]
  Geom
  (guess-scalars [this aes-mappings]
    {:x (if (= :x direction) (scale/default-scalar :compose)
                             (if (utils/factor? (:x aes-mappings))
                               (scale/guess-scalar (:x aes-mappings))
                               (scale/default-scalar :lin-min-max-zero)))
     :y (if (= :y direction) (scale/default-scalar :compose)
                             (if (utils/factor? (:y aes-mappings))
                               (scale/guess-scalar (:y aes-mappings))
                               (scale/default-scalar :lin-min-max-zero)))
     :fill (optional-aesthetic (:fill aes-mappings) (scale/default-scalar :factor))
     :color (optional-aesthetic (:color aes-mappings) (scale/default-scalar :factor))})
  (->svg [this geom-data]
    (let [{[x1 x2] :x [y1 y2] :y :keys [color fill]
           :or {color conf/*color* fill conf/*fill*}} geom-data]
      (when (and x1 x2 y1 y2)
        [:rect {:x (min x1 x2) :width (Math/abs (- x1 x2))
                :y (min y1 y2) :height (Math/abs (- y1 y2))
                :stroke color :fill fill}])))
  (adj-position
    [this sql-data]
    (position-bars this sql-data)))

(defn- mean-if-vec [x]
  (if (coll? x) (utils/mean x) x))

(defrecord Point
  []
  Geom
  (guess-scalars [this aes-mapping]
    (merge
      (utils/fmap scale/guess-scalar (select-keys aes-mapping [:x :y]))
      (utils/fmap (fn [mapping]
                    (optional-aesthetic mapping (scale/guess-scalar mapping)))
        (select-keys aes-mapping [:size :fill :color]))))
  (->svg [this point-data]
    (let [{:keys [x y size color fill]
           :or {size 3, color "gray", fill "white"}} point-data]
      (when (and x y)
        [:circle {:cx (mean-if-vec x)
                  :cy (mean-if-vec y)
                  :r size
                  :stroke color
                  :fill fill}])))
  (adj-position [this sql-data] sql-data))

(defrecord Bin2D
  []
  Geom
  (guess-scalars [this aes-mapping]
    {:x (scale/guess-scalar (:x aes-mapping))
     :y (scale/default-scalar :factor)
     :fill (scale/default-scalar :lin-min-max)})
  (adj-position [this data] data)
  (->svg [this geom-data]
    (let [{[x1 x2] :x [y1 y2] :y :keys [color fill]
           :or {color "#777777" fill "#777777"}} geom-data]
      [:rect {:x (min x1 x2) :width (utils/abs (- x1 x2))
              :y (min y1 y2) :height (utils/abs (- y1 y2))
              :color color :fill fill}])))

(defrecord Path
  []
  Geom
  (guess-scalars [this aes-mapping]
    (into {} (map (fn [[_ m]] (scale/guess-scalar m)) aes-mapping)))
  (adj-position
    [this sql-data]
    (map
      (fn pair-xy [{x1 :x y1 :y :as datum} {x2 :x y2 :y}]
        (-> datum (assoc :x [(utils/->point x1) (utils/->point x2)])
          (assoc :y [(utils/->point y1) (utils/->point y2)])))
      sql-data (rest sql-data)))
  (->svg
    [this {[x1 x2] :x
           [y1 y2] :y
           color :color
           alpha :alpha
           :or {color "black" alpha 1}}]
    [:line {:x1 x1 :x2 x2 :y1 y1 :y2 y2 :stroke color}]))

(defrecord Tick
  [direction]
  Geom
  (guess-scalars [this aes-mapping]
    #?(:clj (throw (Exception. "Not implemented"))))
  (adj-position [this data] data)
  (->svg [this {x :x y :y color :color :or {:color "black"}}]
    (if (= :x direction)
      [:line {:x1 x :x2 x :y1 0 :y2 y :stroke color}]
      [:line {:x1 0 :x2 x :y1 y :y2 y :stroke color}])))

(defrecord Text
  [svg-attrs]
  Geom
  (guess-scalars [this aes-mapping] #?(:clj (throw (Exception. "Not implemented"))))
  (adj-position [this data] #?(:clj (throw (Exception. "Not implemented"))))
  (->svg [this {x :x, y :y, text :text}]
    [:text (merge {:x (utils/->point x) :y (utils/->point y)
                   :font-family conf/*font-family*
                   :font-size conf/*font-size*} svg-attrs) text]))

(defn default-geom
  "Returns a geom based on the kw"
  [kw-geom]
  (case kw-geom
    :histogram (->Bar :x [[:fill :stack]])
    :stacked-bar (->Bar :x [[:fill :stack]])
    :dodged-bar (->Bar :x [[:fill :dodge]])
    :point (->Point)
    :bin2d (->Bin2D)
    :path (->Path)))
