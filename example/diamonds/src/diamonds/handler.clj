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
;; Use the `defdataset` macro to register the diamonds database; see diamonds.data

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

#_(goog-bq/set-credential! "blahblahblah@developer.gserviceaccount.com"
                         "mysupersecretkey.p12")
#_(goog-bq/set-project-id! "my-project-id")
#_(require 'xvsy.datasets) ; the natality dataset should now appear in the web ui



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Example plots using a ggplot2 like syntax.
;; Try changing some of these! They are served at
;; http://localhost:3000/plot-1, http://localhost:3000/plot-2, etc.

(defn plot-1
  "Sometimes you just can't beat a scatter plot. Notice the
   discontinuity at ~$1500 and how strongly clustered the data are at
   0.5, 1, 1.5 and 2.0 carats."
  []
  (let [my-json (qspec :diamonds :point
                       :aes [(x CARAT)
                             (color CUT :id :no-group true)
                             (size 2)
                             (y PRICE :id)]
                       :where [["<" :CARAT 3]])]
    (conf/with-conf {:plot-padding [50 100 20 50]
                     :facet-padding [30 30 0 50]
                     :color cbrew/BrBG-5}
      (xvsy.core/plot-svg 850 500 true my-json))))

(defn plot-2
  "Heatmap showing ~$1500 cut off for diamonds < 0.5 carats, $2000
  cutoff for diamonds < 0.7 carat"
  []
  (let [my-json (qspec :diamonds :bin2d
                       :aes [(x CARAT :bin :lower 0.3 :upper 1.5 :nbins 24)
                             (fill CARAT :count)
                             (y PRICE :bin :lower 0 :upper 3000 :nbins 20)]
                       :where [["<" :PRICE 3000] ["<" :CARAT 1.5] [">" :CARAT 0.30]])]
    (conf/with-conf {:fill cbrew/Blues-6
                     :x-legender nil #_(xvsy.legend/produce-vertical-labels
                                  (fn [[[x & _] & _]] (str x)))}
      (xvsy.core/plot-svg 800 600 true my-json))))

(defn plot-3
  "Example of putting SQL statements into the plot spec. Want good
  value? Buy a diamond that is 0.95 carats"
  []
  (let [my-json
        (qspec :diamonds :dodged-bar
               :aes [(x CARAT :bin :lower 0 :upper 5.5 :nbins 55)
                     (y (non-factor "AVG(PRICE / CARAT)") :sql)
                     #_(facet_y COLOR)
                     #_(fill CUT)])]
    (conf/with-conf {:plot-padding [50 0 0 50]
                     :facet-padding [30 0 0 50]
                     :x-legender (xvsy.legend/produce-vertical-labels
                                  (fn [[[x & _] & _]] (str x)))}
      (xvsy.core/plot-svg 750 500 true my-json))))

(defn plot-4
  "Just like ggplot2, putting in a constant for an aesthetic (in
  this case \"orange\") causes all geoms to have that value."
  []
  (let [my-json (qspec :diamonds :point
                       :aes [(x CARAT :bin :lower 0 :upper 5.5 :nbins 11)
                             (fill COLOR)
                             (color "orange")
                             (y PRICE :avg)
                             (facet_x COLOR)
                             (facet_y CLARITY)])]
    (conf/with-conf {:fill cbrew/Blues-8 ; this is how you change color-schemes
                     :plot-padding [100 100 100 250]
                     :facet-padding [20 20 10 20]}
      (xvsy.core/plot-svg 1400 1600 true my-json))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Web Serving

(defroutes diamond-plot-examples

  ;; This browser UI is meant to act as standalone data exploration console.
  (GET "/" []
       (ring.util.response/redirect "/index.html"))

;; The embed version of plotting UI is meant for sticking into
  ;; iFrames and embedding on external sites.
  (GET "/embed" []
       (ring.util.response/redirect
        (str "/embed.html?spec="
             (xvsy.core/->urlencode-spec (qspec :diamonds :dodged-bar
                                                :aes [(x CARAT :bin :lower 0 :upper 3 :nbins 30)
                                                      (y PRICE :avg)
                                                      (facet_y COLOR)])))))

  ;; serve the example plots.
  (GET "/plot-1" [] (header (response (plot-1)) "Content-Type" "image/svg+xml"))
  (GET "/plot-2" [] (header (response (plot-2)) "Content-Type" "image/svg+xml"))
  (GET "/plot-3" [] (header (response (plot-3)) "Content-Type" "image/svg+xml"))
  (GET "/plot-4" [] (header (response (plot-4)) "Content-Type" "image/svg+xml")))

(def app
  (routes diamond-plot-examples
          xvsy.handlers/gzipped-app))

(defn -main  [& [port]]
  (diamonds.data/init-diamonds-table)
  (let [port (Integer. (or port (System/getenv "PORT") 3333))]
    (log/info "STARTING SERVER ON PORT: " port)
    (defonce server (jetty/run-jetty #'app {:port port :join? false}))))
