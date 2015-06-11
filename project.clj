(defproject xvsy "0.1.0-SNAPSHOT"
  :description "Integrated data visualization and exploration environment"
  :url "http://davetsao.com/blog/2015-06-01-simple-easy-data-viz.html"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [org.clojure/core.incubator "0.1.3"]
                 [com.keminglabs/c2 "0.2.3"]
                 [hiccup "1.0.5"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-json "0.3.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [korma "0.3.1"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [com.h2database/h2 "1.3.170"]
                 [org.clojure/tools.trace "0.7.5"]
                 [com.keminglabs/vomnibus "0.3.2"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.clojure/algo.monads "0.1.5"]
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-json "0.3.1"]
                 [com.google.api-client/google-api-client "1.18.0-rc"]
                 [com.google.apis/google-api-services-bigquery "v2-rev187-1.19.1"]
                 [com.google.http-client/google-http-client-jackson2 "1.19.0"]
                 [prismatic/schema "0.3.3"]
                 [org.clojars.mikejs/ring-gzip-middleware "0.1.0-SNAPSHOT"]
                 [clj-time "0.9.0"]]
  ;;:resource-paths ["resources/bqjdbc-1.4.jar"]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[javax.servlet/servlet-api "2.5"]
                        [org.clojure/tools.namespace "0.2.10"]
                        [ring-mock "0.1.5"]]
         :plugins [[cider/cider-nrepl "0.9.0-SNAPSHOT"]]}
   :production {:aot :all}
   :uberjar {:aot :all}}
  :main ^:skip-aot xvsy.handlers
  :uberjar-name "xvsy-standalone.jar"
  :min-lein-version "2.0.0"
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"}]
                 ["sonatype-oss-public" {:url "https://oss.sonatype.org/content/groups/public/"}]
                 ["google api services" {:url "http://google-api-client-libraries.appspot.com/mavenrepo"}]]
  :jvm-opts ["-Xmx512m" "-server"])
