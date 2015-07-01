(ns xvsy.handlers
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.walk :as walk])
  (:require [compojure.route :as route]
            [compojure.handler]
            [ring.adapter.jetty :as jetty]
            [vomnibus.color-brewer :as color-brewer]
            [vomnibus.d3-color :as d3-color]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [resource-response response redirect
                                        header]]
            [ring.middleware.gzip :as gzip]
            [ring.middleware.json :refer [wrap-json-response]]
            [compojure.core :refer :all]
            [hiccup.core :refer [html]])
  (:require [xvsy.ui :as ui]
            [xvsy.utils :as utils]
            [xvsy.core :as core]
            [xvsy.ggsql :as ggsql]
            [xvsy.geom :as geom]
            [xvsy.macros :refer [with-conf]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Web routes

(defn schema-api
  [spec]
  (when spec
    (binding [ui/*validate* ui/json-validator-with-defaults]
      (let [data (-> spec json/read-str walk/keywordize-keys)
            {current :data schema :schema} (ui/validate-spec data)
            react-schema (ui/reactify-schema schema)]
        (doall (-> {:current (ui/remove-sql-quotes current) :schema react-schema}))))))

(defroutes api
  (ring.middleware.json/wrap-json-body
   (GET "/api/v1/schema"
        [spec :as req]
        (response (schema-api spec))))

  (GET "/api/v1/plot"
       [spec width height inline :as req]
       (binding [ui/*validate* ui/json-validator]
         (let [data (-> spec json/read-str walk/keywordize-keys utils/remove-nils)
               {coerced-spec :data schema :schema} (ui/validate-spec data)
               aesthetics (:aesthetics coerced-spec)
               w (if (string? width) (Integer/parseInt width) width)
               w (if (< w 100) 1400 w)
               h (if (string? height) (Integer/parseInt height) height)
               h (if (< h 100) 800 h)]
           (with-conf {:plot-padding [50 125 10 50]
                            :facet-padding [30 10 10 30]
                            :geom (geom/default-geom (:geom coerced-spec))
                            :x-label (core/x-label (:x aesthetics))
                            :y-label (core/y-label (:y aesthetics))
                            :fill-label (core/aes-label (:fill aesthetics))
                            :color-label (core/aes-label (:color aesthetics))}
             (-> (core/plot-svg w h inline coerced-spec) response
                 (header "Content-Type" "image/svg+xml"))))))

  (GET "/api/v1/head"
       [dataset :as req]
       (let [head (ggsql/m-exec (core/head-dataset dataset))
             ->row (fn [m] [:tr (map (fn [[col val]] [:td val]) m)])
             headings [:tr (map (fn [[col val]] [:th col]) (first head))]]
         (html (conj (map ->row head) headings))))

  ;; FIXME:
  #_(GET "/api/v1/cols_summary"
       [dataset :as req]
       (let [head (ggsql/m-exec (core/head-dataset dataset))
             ->row (fn [m] [:tr (map (fn [[col val]] [:td val]) m)])
             headings [:tr (map (fn [[col val]] [:th col]) (first head))]]
         (html (conj (map ->row head) headings)))))

(defroutes front-end-app
  (GET "/" []
       (redirect "/xvsy.html"))
  (GET "/embed" []
       (redirect "/embed.html"))
  (route/resources "/")
  (route/not-found "404 not found"))

(def gzipped-app (routes (-> api
                             ring.middleware.params/wrap-params
                             ring.middleware.json/wrap-json-response
                             gzip/wrap-gzip)
                         front-end-app))

(defn -main  [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 5000))]
    (log/info "STARTING SERVER ON PORT: " port)
    (defonce server (jetty/run-jetty #'gzipped-app {:port port :join? false}))))
