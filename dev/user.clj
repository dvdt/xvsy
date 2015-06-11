(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.repl :refer [doc source]]
            [clojure.pprint :refer [pprint pp]]
            [clojure.stacktrace :as st]
            [clojure.main]))

(defmacro dbg [x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn contextual-eval [ctx expr]

  (eval

   `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]

      ~expr)))

(defmacro local-context []

  (let [symbols (keys &env)]

    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(defn readr [prompt exit-code]

  (let [input (clojure.main/repl-read prompt exit-code)]

    (if (= input ::tl)

      exit-code

      input)))

;;make a break point

(defmacro break []

  `(clojure.main/repl

    :prompt #(print "debug=> ")

    :read readr

    :eval (partial contextual-eval (local-context))))
