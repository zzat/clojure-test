(defproject subreddit-monitor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.seancorfield/next.jdbc "1.3.894"]
                 [org.xerial/sqlite-jdbc "3.43.2.0"]
                 [com.github.seancorfield/honeysql "2.4.1066"]
                 [clj-http "3.12.3"]
                 [com.zaxxer/HikariCP "3.3.1"]
                 [org.clojure/data.json "2.4.0"]]
  :main ^:skip-aot subreddit-monitor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
