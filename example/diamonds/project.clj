(defproject diamonds "0.1.0-SNAPSHOT"
  :description "Demo of xvsy"
  :url ""
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.3.13"]
                 [reagent "0.5.0"]
                 [reagent-forms "0.5.1"]
                 [reagent-utils "0.1.4"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [hiccup "1.0.2"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [com.h2database/h2 "1.3.170"]
                 [org.clojure/data.csv "0.1.2"]
                 [korma "0.3.1"]
                 [clj-time "0.9.0"]
                 [prismatic/schema "0.3.3"]
                 [xvsy  "0.1.1-SNAPSHOT"]]
  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]
  :plugins [[lein-ring "0.9.5"]
            [lein-cljsbuild "1.0.5"]
            [figwheel "0.3.6"]]
  :ring {:handler diamonds.handler/app
         :init diamonds.data/init-diamonds-table}
  :cljsbuild {:builds {:app {:source-paths ["src"]
                             :figwheel {:on-jsload "diamonds.core/init!"}
                             :compiler {:main diamonds.core
                                        :output-to "resources/public/js/compiled/app.js"
                                        :output-dir "resources/public/js/compiled/out"
                                        :asset-path "js/compiled/out"
                                        :optimizations :none
                                        :pretty-print  true
                                        :source-map true}}}}
  :profiles
  {:dev {:plugins [[lein-figwheel "0.3.3"]
                   [lein-cljsbuild "1.0.6"]]
         :figwheel {:http-server-root "public"
                    :server-port 3449
                    :nrepl-port 7888
                    :css-dirs ["resources/public/css"]
                    :ring-handler diamonds.handler/app
                    }


         :dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.10"]]
         :jvm-opts [] #_["-Dcom.sun.management.jmxremote"
                    "-Dcom.sun.management.jmxremote.ssl=false"
                    "-Dcom.sun.management.jmxremote.authenticate=false"
                    "-Dcom.sun.management.jmxremote.port=43210"]}
   :uberjar {:aot :all}}
  :aot []
  :main ^:skip-aot diamonds.handler)
