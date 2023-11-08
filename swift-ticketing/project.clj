(defproject swift-ticketing "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aero "1.1.6"]
                 [ring/ring-core "1.11.0-RC1"]
                 [ring/ring-jetty-adapter "1.11.0-RC1"]
                 [compojure "1.7.0"]
                 [ring/ring-defaults "0.3.2"]
                 [com.stuartsierra/component "1.1.0"]
                 [com.github.seancorfield/next.jdbc "1.3.894"]
                 [org.xerial/sqlite-jdbc "3.43.2.0"]
                 [com.github.seancorfield/honeysql "2.4.1066"]
                 [com.zaxxer/HikariCP "3.3.1"]
                 [org.postgresql/postgresql "42.6.0"]]
  :main ^:skip-aot swift-ticketing.core
  :target-path "target/%s"
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler swift-ticketing.app/swift-ticketing-app}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
