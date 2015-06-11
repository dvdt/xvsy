(ns xvsy.utils
  (:require [clojure.algo.generic.functor :refer [fmap]])
  (:require [clojure.set])
  (:require [clojure.walk])
  (:require [schema.core :as s])
  (:require [clj-time.coerce]))

(defn css
  "inspired by hiccup example: (css [\"div\" \".my-class\"] {:font \"10px sans-serif\"})
  -> \"div .my-class {
         font: 10px sans-serif;\""
  ([selector style]
     (let [selector-str (apply str (map #(str % " ") selector))
           style-str (reduce (fn [col [k v]]
                               (str "  " col (name k) ": " v ";" \newline)) "" style)]
       (str selector-str \{ \newline style-str \})))
  ([selector style & more]
     (str (css selector style) \newline (apply css more))))

(defn cumsum [a] (reductions + a))

(defn split-apply-combine
  "split, apply, combine on a seq of maps"
  [data & [split-fn apply-fn combine-fn]]
  (let [split (if (nil? split-fn) identity (fn [d] (group-by split-fn d)))
        apply  (if (nil? apply-fn) identity (fn [data-split] (fmap #(fmap apply-fn %) data-split)))
        combine (if (nil? combine-fn) identity (fn [data-apply] (fmap combine-fn data-apply)))]
    (-> data split apply combine)))

(defn apply2 [g fn args] (apply fn (conj args g)))

(defn mapmap
 "(mapmap {:a inc :b dec} {:a 1 :b 1}) => {:a 2 :b 0}"
 [fns & maps]
 (reduce (fn [acc, k] (if (k fns) (update-in acc [k] (fns k)) acc)) vals
         (keys vals)))

(defn apply-map
  "Returns a map as a result of calling f with the arguments given in the maps.
  Similar to merge-with."
  [f & hashmaps]
  (let [ks (apply clojure.set/intersection (map (comp set keys) hashmaps))

        ]
    (zipmap ks (map (fn [k]
                      (apply f (map #(get % k) hashmaps))) ks))))

(defn apply-key-map
  "Similar to apply-map, but the function expects the key to be the first arg"
  [f & hashmaps]
  (let [key-map (zipmap (keys (first hashmaps)) (keys (first hashmaps)))]
    (apply apply-map f key-map hashmaps)))

(defn uniqify [s]
  (loop [seen (set nil)
         remaining s
         uniqs []]
    (if (empty? remaining)
      uniqs
      (let [current (first remaining)
            new-seen (conj seen current)
            new-uniqs (if (seen current) uniqs (conj uniqs current))]
        (recur new-seen (rest remaining) new-uniqs)))))

(defmacro time2
  "Evaluates expr and prints the time it took.  Returns the value of
  expr."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str "Elapsed time: "  (str '~expr) \ (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(defn zip
  "Equivalent to zip in python."
[seqs]
(apply map list seqs))

(defn mean
  "computes the arithmetic mean"
  [coll]
  (/ (apply + coll) (count coll)))

(defn to-columnar
  "Turns a list of maps into a map of lists.
  (to-columnar [{:a 1 :b 9} {:a 2 :b 8}]) => {:a [1 2], :b [9 8]}"
  [hmaps]
  (reduce #(assoc %1 %2 (map %2 hmaps)) {} (-> hmaps first keys)))

(defn to-row
  [map-of-colls]
  "Turns a map of lists to a list of structs"
  (let [basis (apply create-struct (keys map-of-colls))]
    (reduce (fn bind-row-data
              [acc, [k data]]
              (map #(assoc %1 k %2) acc data))
            (repeat (struct basis))
            map-of-colls)))

(defn get-many
  [keys]
  (fn [x] (map #(% x) keys)))

(defn ->point
  "Returns the mean of an interval, or the point"
  [interval-or-point]
  {:pre [(or (and (coll? interval-or-point)
                  (= (count interval-or-point) 2))
             (number? interval-or-point))]}
  (if (coll? interval-or-point) (mean interval-or-point) interval-or-point))

(defn log-most-sig-digit
  ""
  ([num] (log-most-sig-digit num 10))
  ([num base]
   (-> num Math/log (/ (Math/log base)) Math/floor int)))

(defn log-most-sig-digit
  ""
  ([num] (log-most-sig-digit num 10))
  ([num base]
   (-> num Math/log (/ (Math/log base)) Math/floor int)))

(defn nums->str
  "Returns a list of strings representing the numbers, with precision
   up to the most-significant-digit - 1 in the differences between the
   nums.
  EG: [1.0 2.0 3.0] [\"1\" \"2\" \"3\"], while [1.100 1.2 1.4] ->
  [\"1.1\" \"1.2\" \"1.4\"]"
  [nums]
  (let [absolute-diffs (map #(Math/abs (- %1 %2)) nums (rest nums))
        smallest (apply min absolute-diffs)
        log-smallest (- (log-most-sig-digit smallest) 1)
        ]
    (cond
      (< log-smallest -3) (map (partial format "%.1e") nums)
      (< log-smallest 0)
      (map (comp (partial format (str "%." (Math/abs log-smallest) \f)) double) nums)
      (> log-smallest 4) (map (comp (partial format "%.0e") double) nums)
      :else (map (comp str int) nums)
      )))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn flatten-kv
  "Returns a seq of flattened keys and value pairs."
  [[k v]]
  (if-not (map? v)
    (list [(list k) v])
    (map (fn [[k- v-]]
           [(conj k- k) v-])
         (mapcat flatten-kv v))))

(defn flatten-map
  "given a nested map, returns a non-nested map with nested keys
  represented as a seq."
  [m]
  (into {} (mapcat flatten-kv m)))

(defn pair?
  "Returns true for 2 element vector"
  [p]
  (and (vector? p) (= (count p) 2)))

(defn desconce
  [ensconce-char string]
  (if (and (= (first string) ensconce-char)
           (= (last string) ensconce-char))
    (.substring string 1 (- (count string) 1))
    string))

(defn ensconce
  [ensconce-char string]
  (if (and (= (first string) ensconce-char)
           (= (last string) ensconce-char))
    string
    (str ensconce-char string ensconce-char)))

(defn ensconced?
  [ensconce-char val]
  (= (ensconce ensconce-char val) val))

(defn remove-nils
  "Removes keys from a map thaqt map to a nil val"
  [data]
  (let [rm-nil
        #(if (map? %)
           (into {} (filter (comp not nil? second) %))
           %)]
    (clojure.walk/prewalk rm-nil data)))

(defn optional-entry?
  "returns true if entry in a hashmap has an optional key"
  [x]
  (and (pair? x) (s/optional-key? (first x))))

(defmacro remap
  "\"restructures\" a map. For example:
  (let [a 1 b 2 c 3]
  (remap a b c))
  => {:a 1 :b 2 :c3}"
  ([a]
   `(hash-map (keyword '~a) ~a))
  ([a b]
   `(hash-map (keyword '~a) ~a (keyword '~b) ~b))
  ([a b c]
   `(hash-map (keyword '~a) ~a (keyword '~b) ~b (keyword '~c) ~c))
  ([a b c d]
   `(hash-map (keyword '~a) ~a (keyword '~b) ~b (keyword '~c) ~c
              (keyword '~d) ~d))
  ([a b c d e]
   `(hash-map (keyword '~a) ~a (keyword '~b) ~b (keyword '~c) ~c
              (keyword '~d) ~d (keyword '~e) ~e))
  ([a b c d e f]
   `(hash-map (keyword '~a) ~a (keyword '~b) ~b (keyword '~c) ~c
              (keyword '~d) ~d (keyword '~e) ~e (keyword '~f) ~f)))
