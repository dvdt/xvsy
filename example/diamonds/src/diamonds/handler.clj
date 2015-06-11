(ns diamonds.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :as jetty]
            [schema.core :as s]
            [xvsy.ggsql :refer [defdataset]]
            [xvsy.handlers]
            [xvsy.goog-bq :as goog-bq]
            [diamonds.data])
  (:gen-class :main true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Use the `defdataset` macro to register the diamonds database

(defdataset ggplot2-diamonds "diamonds"
  (korma.core/table "DIAMONDS")
  (korma.core/database diamonds.data/diamonds-db)
  (assoc :cols
         {"ID" {:factor false :type s/Int},
          "CARAT" {:factor false :type s/Num},
          "CUT" {:factor true :type s/Str},
          "COLOR" {:factor true :type s/Str},
          "CLARITY" {:factor true :type s/Str},
          "DEPTH" {:factor false :type s/Num},
          "TABLE" {:factor false :type s/Int},
          "PRICE" {:factor false :type s/Int},
          "X" {:factor false :type s/Num},
          "Y" {:factor false :type s/Num},
          "Z" {:factor false :type s/Num}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Google Big Query: if you have a google big query account, set your
;; credentials and load xvsy.datasets

;; (goog-bq/set-credential! "blahblahblah@developer.gserviceaccount.com"
;;                          "mysupersecretkey.p12")
;; (goog-bq/set-project-id! "my-project-id")
;; (require 'xvsy.datasets) ; the natality dataset should now appear in the web ui

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Web routes

(def app
  xvsy.handlers/gzipped-app)

(defn -main  [& [port]]
  (diamonds.data/init-diamonds-table)
  (let [port (Integer. (or port (System/getenv "PORT") 3333))]
    (log/info "STARTING SERVER ON PORT: " port)
    (defonce server (jetty/run-jetty #'app {:port port :join? false}))))
