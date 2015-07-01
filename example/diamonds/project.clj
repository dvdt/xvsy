(defproject diamonds "0.1.0-SNAPSHOT"
  :description "Demo of xvsy"
  :url ""
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
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
  :plugins [[lein-ring "0.9.5"]]
  :ring {:handler diamonds.handler/app
         :init diamonds.data/init-diamonds-table}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.10"]]
         :jvm-opts [] #_["-Dcom.sun.management.jmxremote"
                    "-Dcom.sun.management.jmxremote.ssl=false"
                    "-Dcom.sun.management.jmxremote.authenticate=false"
                    "-Dcom.sun.management.jmxremote.port=43210"]}
   :uberjar {:aot :all}}
  :aot []
  :main ^:skip-aot diamonds.handler)
