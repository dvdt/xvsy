(ns xvsy.ggsql
  (:require
   [clojure.tools.logging :as log]
   [xvsy.stat :refer [subprotocol]]
   [xvsy.goog-bq]
   [korma.core]))

;; atom that maps a str dataset name to a dataset.
(defonce datasets
  (atom {}))

(defn exec
  "Similar to korma.core/exec, but, dispactches on the subprotocol in order to
   accomodate non jdbc compatible dbs (e.g. google big query)."
  [query]
  (log/info "Executing query: " (korma.core/as-sql query) "::" (:params query))
  (cond
    (= "goog-bq" (subprotocol query)) (xvsy.goog-bq/exec-bq query)
    :else (korma.core/exec query)))

(def m-exec (memoize exec))

(defmacro defdataset
  "Use this to define your own datasets."
  [name str-name & body]
  `(do
     (korma.core/defentity ~name
       ~@body)
     (swap! datasets #(assoc % ~str-name ~name))))
