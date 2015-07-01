(ns xvsy.macros)

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

(defmacro doall-time
  [name & body]
  `(do
     (print ~name)
     (let [res# ~@body]
       (if (coll? res#)
         (time (doall res#))
         (do (print \newline) res#)))))

(defn kw->var
  [kw]
  (intern 'xvsy.conf (symbol (str \* (name kw) \*))))

(defmacro with-conf
  [bindings & body]
  (let [starred-vars (map kw->var (keys bindings))
        starred-bindings (zipmap starred-vars (vals bindings))]
    `(clojure.core/with-bindings ~starred-bindings ~@body)))
