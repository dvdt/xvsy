(ns xvsy.scale
  "Namespace for mapping data onto aesthetic (i.e. x, y, color, etc.)
  coordinates."
  (:require [clojure.tools.logging :as log]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [c2.ticks :as ticks]
            [vomnibus.color-brewer :as color-brewer]
            [xvsy.conf]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocols and Multimethods

(defprotocol Scalar
  "Represents training data to a scale. A scalar is trained on data and can
  generate a scale. Importantly, scalars can be appended together."
  (train ^Scalar [this data] "Train the scalar on the given data. Returns a new scalar")
  (->scale [this range]
    "Produces a scale fn that takes returns data normalized to the given range."))

(defprotocol Ticker
  "Produces aesthetic tick marks. Often used in conjunction with scalar."
  (->ticks [this] "Returns a guess of tick marks."))

(defmulti guess-svg-scale
  "Returns a function of type: scalar -> svg-scale. Define methods to set default scales.
  args: [aes scalar]"
  (fn [aes scalar] [(keyword "xvsy.scale" (name aes)) (class scalar)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Records

(defn factor-fn
  "maps keywords onto an interval, uniformly."
  [factors interval]
  (let [n (count factors)
        [r1 r2] (map double interval)
        factor-width (/ (- r2 r1) n)
        factor-start (zipmap factors (range r1 r2 factor-width))
        factor-end (zipmap factors (range (+ r1 factor-width) (+ r2 factor-width) factor-width))]
    (merge-with vector factor-start factor-end)))

(defn linear
  "maps d in domain to range, linearly"
  [domain range d]
  (if (nil? d) nil
      (let [[d1 d2 r1 r2] (map double (concat domain range))
            slope (/ (- r2 r1) (- d2 d1))]
        (if (coll? d)
          (let [[da db] (map double d)]
            [(-> (- da d1) (* slope) (+ r1)) (-> (- db d1) (* slope) (+ r1))])
          (-> (- d d1) (* slope) (+ r1))))))

(defn interval-or-point-min-max
  "Returns [min max] in xs, where xs is either [[x1 x2] [x3 x4] ...]
   or [x1 x2 ...]"
  [xs]
  (let [sorted-vals (sort xs) ;; exploits clojure compare to sort
        ;; int to int, [y1 y2] to [y2 y3]
        min-int? (first sorted-vals)
        min-actual (if (coll? min-int?) (apply min min-int?) min-int?)
        max-int? (last sorted-vals)
        max-actual (if (coll? max-int?) (apply max max-int?) max-int?)]
    [min-actual max-actual]))

;; LinMinMaxScalar is for real numbers. It maps the minimum value seen
;; to the bottom of the range, and the maximum value seen to the top
;; of the range.
(defrecord LinMinMaxScalar
    [minimum maximum]
  Scalar
  (train [this data]
    (let [non-nil-data (filter (comp not nil?) data)
          [data-min data-max] (interval-or-point-min-max non-nil-data)
          new-min (min data-min minimum)
          new-max (max data-max maximum)]
      (->LinMinMaxScalar new-min new-max)))
  (->scale [this range] (partial linear [minimum maximum] range))
  Ticker
  (->ticks [this] (:ticks (ticks/search [minimum maximum]))))


;; FactorScalar keeps track of previously seen values and can produce
;; a scale that maps a factor (e.g. a string or keyword) to an
;; interval.
(defrecord FactorScalar
    [^clojure.lang.IPersistentSet seen-vals factor-comparator]
  Scalar
  (train [this data]
    (FactorScalar. (into seen-vals data) factor-comparator))
  (->scale [this range]
    (factor-fn (sort factor-comparator seen-vals) range))
  Ticker
  (->ticks [this] (sort factor-comparator seen-vals)))


;; ComposeScalar is for composite scalars (i.e. scalars made of
;; scalars). This situation arises, for example, when we have
;; multi-level factor data, like ["US" "Texas"] ["UK" "Scotland"].
(defrecord ComposeScalar
    [scalars ranges]
  Scalar
  (train
    [this data]
    (let [transposed-data (apply map list data)
          updated-scalars (map train scalars transposed-data)
          updated-ranges (take (count updated-scalars) ranges)]
      (ComposeScalar. updated-scalars updated-ranges)))
  (->scale
    [this final-range]
    (let [scales (map ->scale scalars ranges)
          compose-scales (fn [domain, v] (linear [0.0 1.0] domain v))]
      (fn compose-to-scale [val] (reduce compose-scales final-range (map #(%1 %2) scales val)))))
  Ticker
  (->ticks [this]
    (apply cartesian-product (map ->ticks scalars))
    (map vector (->ticks (first scalars)))))


;; ModuloScalar produces a scale that is (factor-index % modulus)
(defrecord ModuloScalar
    [^FactorScalar factor-scalar modulus]
  Scalar
  (train [this data]
    (ModuloScalar. (train factor-scalar data) modulus))
  (->scale [this interval]
    (let [ordered-factors (sort (:factor-comparator factor-scalar)
                                (:seen-vals factor-scalar))
          int-to-interval (factor-fn (range modulus) interval)
          factor-to-interval (zipmap
                              ordered-factors
                              (map (comp int-to-interval
                                         #(mod % modulus))
                                   (range (count ordered-factors))))]
      factor-to-interval))
  Ticker
  (->ticks [this] (->ticks factor-scalar)))


;; Produces a scale that floors the value of (factor index) / divisor
(defrecord FloorScalar
    [^FactorScalar factor-scalar divisor]
  Scalar
  (train [this data] (FloorScalar. (train factor-scalar data) divisor))
  (->scale [this interval]
    (let [ordered-factors (sort (:factor-comparator factor-scalar)
                                (:seen-vals factor-scalar))
          int-to-interval (factor-fn
                           (range (-> (count ordered-factors) (- 1) (/ divisor)
                                      int (+ 1))) interval)
          factor-to-interval (zipmap
                              ordered-factors
                              (map (comp int-to-interval
                                         #(int (/ % divisor)))
                                   (range (count ordered-factors))))]
      factor-to-interval))
  Ticker
  (->ticks [this] (->ticks factor-scalar)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions

(defn color-continuous
  "maps x, with domain [0, 1] to the given colors"
  [colors x]
  (let [n (count colors)
        bin (int (* n x))
        bin (if (== n bin) (- bin 1) bin)]
    (nth colors bin)))

(defn default-scalar
  "Returns a default (i.e. monoid empty) scalar, or constructs it with
  the given args."
  ([scalar-type]
   (case scalar-type
     :lin-min-max (->LinMinMaxScalar Long/MAX_VALUE Long/MIN_VALUE)
     :lin-min-max-zero (->LinMinMaxScalar 0 0)
     :factor (->FactorScalar #{} compare)
     :modulo (->ModuloScalar (default-scalar :factor) 4)
     :compose (->ComposeScalar (repeat 9 (default-scalar :factor))
                               (conj (repeat 8 [0.1 0.9]) [0 1]))
     :floor (->FloorScalar (default-scalar :factor) 4)))
  ([scalar-type & args]
   (case scalar-type
     :lin-min-max (apply ->LinMinMaxScalar args)
     :lin-min-max-zero (apply ->LinMinMaxScalar args)
     :factor (apply ->FactorScalar args)
     :modulo (apply ->ModuloScalar args)
     :compose (apply ->ComposeScalar args)
     :floor (apply ->FloorScalar args))))

(defn factor-comparator
  "Returns a comparator that respects the ordering of the given factors"
  [factors]
  (let [factor-to-int (zipmap factors (range))]
    #(- (factor-to-int %1) (factor-to-int %2))))

(defn wrap-square
  "Computes number of rows and cols for  n tiles wrapped in a 2D space.
  Attempts to make row ~ col.
  Returns [ncols nrows]"
  [n]
  (let [sqrt-n (-> n Math/sqrt Math/ceil)
        cols (int sqrt-n)
        rows (int (Math/ceil (/ n sqrt-n)))]
    [cols rows]))

(defn train-global-scalars
  "Trains a scalar across all data given, and returns a list of that
scalar.  [facet-data], scalar => [trained-scalar]."
  [init-scalar facet-data-coll]
  (repeat (count facet-data-coll)
          (reduce (fn [s, column-data]
                    (train s column-data)) init-scalar facet-data-coll)))

(defn train-individual-scalars
  "Returns a (possibly) distinct scalar on each data given."
  [init-scalar facet-data-coll ]
  (map #(train init-scalar %) facet-data-coll))

(defn facet-wrap
  "Wraps facets in a grid."
  [facets]
  (let [[c _] (wrap-square (count facets))]
    [(train
      (default-scalar :modulo (default-scalar :factor) c)
      (map first facets))
     (train
      (default-scalar :floor (default-scalar :factor) c)
      (map second facets))]))

(defn facet-grid
  "returns trained facet scalar for the given facets"
  [facets]
  [(train (default-scalar :factor) (map first facets))
   (train (default-scalar :factor) (map second facets))])

(derive ::fill ::color)
(derive ::x ::position)
(derive ::y ::position)
(derive ::facet_x ::position)
(derive ::facet_y ::position)
(derive xvsy.scale.ModuloScalar ::facet-scalar)
(derive xvsy.scale.FloorScalar ::facet-scalar)

(defmethod guess-svg-scale [::position FactorScalar]
  [aes factor-scalar]
  (->scale factor-scalar (or (xvsy.conf/get-conf aes)
                             (throw (Exception. "Factor must have position aesthetic.")))))

(defmethod guess-svg-scale [::color FactorScalar]
  [aes factor-scalar]
  (let [colors (or (xvsy.conf/get-conf aes) color-brewer/Paired-11)
        seen-vals (sort (:factor-comparator factor-scalar) (:seen-vals factor-scalar))
        n (count seen-vals)
        ]
    (zipmap seen-vals (take n (flatten (repeat colors))))))

(defmethod guess-svg-scale [::position ComposeScalar]
  [aes scalar]
  (->scale scalar (or (xvsy.conf/get-conf aes)
                      (throw (Exception. "Conf must have position aesthetic.")))))

(defmethod guess-svg-scale [::position LinMinMaxScalar]
  [aes scalar]
  (->scale scalar (or (xvsy.conf/get-conf aes)
                      (throw (Exception. "scalar must have position aesthetic.")))))

(defmethod guess-svg-scale [::color LinMinMaxScalar]
  [aes scalar]
  (comp (partial color-continuous (or (xvsy.conf/get-conf aes) color-brewer/Blues-9))
        (->scale scalar [0 1])))

(defmethod guess-svg-scale [::position ::facet-scalar]
  [aes scalar]
  (->scale scalar (or (xvsy.conf/get-conf aes)
                      (throw (Exception. "scalar must have position aesthetic.")))))

(defn guess-scalar
  "Returns a scalar for the aes-mapping"
  [mapping]
  (case [(get-in mapping [:col :factor]) (get-in mapping [:stat :name])]
    [true :id] (default-scalar :factor)
    [false :bin] (default-scalar :factor)
    ;; default
    (default-scalar :lin-min-max-zero)))

(defn guess-svg-scales
  "Given a map of aes => scalar, returns a map of aes => scale"
  [aes-scalar]
  (apply hash-map (mapcat
                   (fn [[aes scalar]] [aes (guess-svg-scale aes scalar)])
                         aes-scalar)))
