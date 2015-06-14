(ns diamonds.data
  "Setup an H2 database containing the diamonds dataset."
  (:require
   [clojure.java.jdbc :as sql]
   [korma core db]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]))

(def diamonds-db {:classname "org.h2.Driver"
                  :subprotocol "h2:file"
                  :subname "diamonds-db"})

(defn slurp-diamonds
  "Returns diamonds dataset from diamonds.csv as a list of column values."
  []
  (with-open [diamonds-file (io/reader (io/resource "diamonds.csv"))]
    (doall
     (rest (csv/read-csv diamonds-file)))))

(defn init-diamonds-table
  "Creates diamonds table and fills it with diamonds.csv data"
  []
  (try (sql/db-do-commands diamonds-db (sql/drop-table-ddl :diamonds))
       (catch org.h2.jdbc.JdbcBatchUpdateException e))
  (sql/db-do-commands
   diamonds-db
   (sql/create-table-ddl :diamonds
                         [:id "int primary key auto_increment"]
                         [:carat "real"]
                         [:cut "varchar(32)"]
                         [:color "varchar(32)"]
                         [:clarity "varchar(32)"]
                         [:depth "real"]
                         [:table "real"]
                         [:price "real"]
                         [:x "real"]
                         [:y "real"]
                         [:z "real"]))
  (apply sql/insert! diamonds-db :diamonds
               ["id" "carat" "cut" "color" "clarity" "depth" "table" "price" "x" "y" "z"]
               (slurp-diamonds)))
