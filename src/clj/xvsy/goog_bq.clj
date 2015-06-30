(ns xvsy.goog-bq
  "Client code for querying Google Big Query"
  (:require
   [clojure.edn :as edn]
   [clojure.algo.generic.functor :only [fmap]]
   [clojure.string :as string]
   [korma.db :as db]
   [korma.core]
   [clojure.tools.logging :as log]
   [schema.core :as s]
   [clj-time.coerce])
  (:import
   [org.joda.time DateTime]
   [com.google.api.client.http.javanet NetHttpTransport]
   [com.google.api.client.http HttpTransport]
   [com.google.api.client.json JsonFactory]
   [com.google.api.client.json.jackson2 JacksonFactory]
   [com.google.api.client.googleapis.auth.oauth2 GoogleCredential
    GoogleCredential$Builder]
   [com.google.api.client.util Data]
   [com.google.api.services.bigquery BigqueryScopes
    Bigquery Bigquery$Builder Bigquery$Datasets Bigquery$Tables]
   [com.google.api.services.bigquery.model Dataset DatasetList Job
    TableSchema JobConfiguration JobConfigurationQuery QueryRequest
    QueryResponse TableCell TableRow]))

(defonce *project-id* (atom nil))

(defonce *JACKSON* (JacksonFactory.))
(defonce *transport* (NetHttpTransport.))

(defonce *credential* (atom nil))

(defn set-project-id!
  "Call before making GBQ requests."
  [project-id]
  (swap! *project-id* (constantly project-id)))

(defn set-credential!
  "Call once before making google big query requests for
  authorization. account-id is the service account email
  address. key-filename is the filename of the .p12 keyfile."
  [account-id key-filename]
  (let [credential (.. (GoogleCredential$Builder.)
                       (setTransport *transport*)
                       (setJsonFactory *JACKSON*)
                       (setServiceAccountId account-id)
                       (setServiceAccountScopes (vector (BigqueryScopes/BIGQUERY)))
                       (setServiceAccountPrivateKeyFromP12File
                        (java.io.File. key-filename))
                       (build))]
    (swap! *credential* (constantly credential))))

;; I would have liked to use this JDBC driver for big query, but
;; it's too buggy. Results with null values throw errors!
;; (def db
;;     {:classname "net.starschema.clouddb.jdbc.BQDriver"
;;      :subprotocol "BQDriver"
;;      :subname  "festive-zoo-697?withServiceAccount=true&transformQuery=true&user=@developer.gserviceaccount.com&password=.p12"})
;; (def stmt (with-open [^java.sql.Connection con (get-connection db)]
;;             (with-open [ stmt (apply prepare-statement con "SELECT * FROM FLIGHTS")]
;;               (.toString stmt))))

(defn ->str
  ^String [s]
  (cond
    (Data/isNull s) nil
    (and (= (first s) \[) (= (last s) \])) (edn/read-string s)
    :else s))

(defn- gq-field-converter
  [field-schematic]
  (let [field-schematic (into {} field-schematic)]
    (case (field-schematic "type")
      "STRING" ->str
      "INTEGER" #(if (Data/isNull %) nil (Long/parseLong %))
      "FLOAT" #(if (Data/isNull %) nil (Double/parseDouble %))
      "BOOLEAN" #(if (Data/isNull %) nil (str (Boolean/parseBoolean %)))
      "TIMESTAMP" #(if (Data/isNull %) nil
                       (-> % Double/parseDouble (* 1000) long clj-time.coerce/from-long))
      "RECORD" (throw (Exception. "Not implemented")))))

(defn ->schematized-maps
  "casts bigquery results into clojure hashmaps"
  [^QueryResponse res]
  {:pre [(.get res "schema")]}
  (let [schema (into {} (.get res "schema"))
        field-keys (map #(keyword (.get % "name")) (schema "fields"))
        struct-basis (apply create-struct field-keys)
        parse-vs (map gq-field-converter (schema "fields"))
        ->row (fn [table-row]
                (let [table-cells (.get table-row "f")
                      vs (map #(%1 (.get %2 "v")) parse-vs table-cells)]
                  vs))
        structs (map (fn ->hashmap-row
                       [row]
                       (apply struct struct-basis (->row row))) (.getRows res))]
    (binding [*print-dup* true]
      (log/debug (str "parsing bq results, first row=" (first structs))))
    structs))

(defn build-bq
  "Returns a Bigquery instance"
  []
  (let [credential @*credential*]
    (.. (Bigquery$Builder. *transport* *JACKSON*  credential)
        (setApplicationName "xvsy/0.1")
        (setHttpRequestInitializer credential)
        (build))))

(defn goog-bq
  "Creates a database specification for Google Big Query. Opts should
  include client-id and client-secret. Delimiters autoset to \"\"."
  [{:keys {:client-id :client-secret}
    :as opts}]
  (merge {:subprotocol "goog-bq"
          :delimiters ""
          :make-pool? false} opts))

(defn exec-bq
  "Executes a bigquery query."
  [query]
  (let [bq (build-bq)
        korma-q (korma.core/query-only (korma.core/exec query))
        sql (:sql-str korma-q)
        params (:params korma-q)
        ;; FIXME: bq does not support parameterized queries.
        ;; look into
        ;; https://www.owasp.org/index.php/SQL_Injection_Prevention_Cheat_Sheet#Defense%5FOption%5F3%3A%5FEscaping%5FAll%5FUser%5FSupplied%5FInput
        ;; to escape user input
        unsafe-param-sql (apply str (-> sql (string/split #"\?")
                                        (interleave (concat params [nil]))))
        _ (log/info (str "GBQ: " unsafe-param-sql))
        q-req (.. (QueryRequest.) (setQuery unsafe-param-sql)
                  (setTimeoutMs 30000))
        q-res (.. bq (jobs) (query @*project-id* q-req) (execute))]
    (if (.getJobComplete q-res)
      (do
        (log/info "bigquery job complete")
        (->schematized-maps q-res))
      (throw (Exception. "Exceeded timeout for google bigquery execution.")))))
