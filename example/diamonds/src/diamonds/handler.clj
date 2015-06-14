(ns diamonds.handler
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET routes]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response header]]
            [schema.core :as s]
            [hiccup.core :refer [html]]
            [xvsy.ggsql :refer [defdataset exec]]
            [xvsy.geom :as geom]
            [xvsy.plot :as plot]
            [xvsy.scale :as scale]
            [xvsy.aesthetics :as aes :refer [x y fill facet_y facet_x color size]]
            [xvsy.handlers]
            [xvsy.goog-bq :as goog-bq]
            [xvsy.conf :as conf]
            [xvsy.core :refer :all ]
            [vomnibus.color-brewer :as cbrew]
            [clojure.algo.generic.functor :refer [fmap]]
            [diamonds.data])
  (:gen-class :main true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Use the `defdataset` macro to register the diamonds database

(defdataset ggplot2-diamonds "diamonds"
  (korma.core/table "DIAMONDS")
  (korma.core/database diamonds.data/diamonds-db)
  (korma.core/transform
   (fn [m]
     (fmap #(if (and (string? %)
                     (= \[ (first %))
                     (= \] (last %)))
              (clojure.edn/read-string %) %)
           m)))
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
;; credentials and load a public dataset

;; (goog-bq/set-credential! "blahblahblah@developer.gserviceaccount.com"
;;                          "mysupersecretkey.p12")
;; (goog-bq/set-project-id! "my-project-id")
;; (require 'xvsy.datasets) ; the natality dataset should now appear in the web ui


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Web routes

(defn plot-1 []
  (let [my-json (qspec :diamonds :point
                       :aes [(x CARAT)
                             (fill "white")
                             (size 2)
                             (y PRICE :id)]
                       :where [["<" :CARAT 3]])]
    (conf/with-conf {}
      (xvsy.core/plot-svg 1400 800 false my-json))))

(defn plot-2 []
  (let [my-json (qspec :diamonds :dodged-bar
                       :aes [(x CARAT :bin :lower 0 :upper 3 :nbins 30)
                             (facet_y CLARITY)
                             (y (non-factor "AVG(PRICE / CARAT)") :sql)]
                      :where [])]
    (conf/with-conf {:fill cbrew/Spectral-8
                     :x-legender (xvsy.legend/produce-vertical-labels
                                  (fn [[[x & _] & _]] (str x)))}
      (xvsy.core/plot-svg 800 1200 true my-json))))

(defn plot-3 []
  (let [my-json
        (qspec :diamonds :dodged-bar
               :aes [(x CARAT :bin :lower 0 :upper 2.5 :nbins 10)
                     (y PRICE :avg)
                     (facet_y COLOR)
                     (fill CLARITY)])]
    (conf/with-conf {:plot-padding [100 100 100 250]
                     :fill cbrew/Blues-8
                     :x-legender (xvsy.legend/produce-vertical-labels
                                  (fn [[[x & _] & _]] (str x)))}
      (xvsy.core/plot-svg 1400 1600 false my-json))))

(defn plot-4 []
  (let [my-json (qspec :diamonds :point
                       :aes [(x CARAT :bin :lower 0 :upper 2.5 :nbins 50)
                             (fill CLARITY)
                             (color COLOR)
                             (y PRICE :avg)
                             (facet_y CLARITY)])]
    (conf/with-conf {:fill cbrew/Blues-8
                     :color nil
                     :plot-padding [100 100 100 250]}
      (xvsy.core/plot-svg 1400 1600 false my-json))))

(defroutes diamond-plot-examples
  ;; simple plots can be specified as JSON. Sane defaults means that
  ;; the plot usually looks reasonably good.
  (GET "/plot-1" [] (header (response (plot-1)) "Content-Type" "image/svg+xml"))
  (GET "/plot-2" [] (header (response (plot-2)) "Content-Type" "image/svg+xml"))
  (GET "/plot-3" [] (header (response (plot-4)) "Content-Type" "image/svg+xml"))
  (GET "/plot-4" [] (header (response (plot-3)) "Content-Type" "image/svg+xml")))

(def app
  (routes diamond-plot-examples
          xvsy.handlers/gzipped-app))

(defn -main  [& [port]]
  (diamonds.data/init-diamonds-table)
  (let [port (Integer. (or port (System/getenv "PORT") 3333))]
    (log/info "STARTING SERVER ON PORT: " port)
    (defonce server (jetty/run-jetty #'app {:port port :join? false}))))
