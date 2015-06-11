(defproject diamonds "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [hiccup "1.0.2"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [com.h2database/h2 "1.3.170"]
                 [org.clojure/data.csv "0.1.2"]
                 [korma "0.3.1"]
                 [clj-time "0.9.0"]
                 [prismatic/schema "0.3.3"]
                 [xvsy  "0.1.0-SNAPSHOT"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler diamonds.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [org.clojure/tools.namespace "0.2.10"]]}
   :uberjar {:aot :all}}
  :aot []
  :main ^:skip-aot diamonds.handler)
