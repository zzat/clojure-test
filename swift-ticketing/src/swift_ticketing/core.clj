(ns swift-ticketing.core
  (:require [swift-ticketing.config :as config]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [ring.adapter.jetty :refer [run-jetty]]
            [swift-ticketing.app :as app]
            [swift-ticketing.worker :as worker])
  (:import (com.zaxxer.hikari HikariDataSource))
  (:gen-class))

(defn create-connection-pool [database-config]
  (connection/->pool com.zaxxer.hikari.HikariDataSource
                     {:dbtype (:dbtype database-config)
                      :dbname (:dbname database-config)
                      :username (:username database-config)
                      :password (:password database-config)
                      :host (:host database-config)
                      :port (:port database-config)
                      :schema (:schema database-config)}))

(defn -main
  [& args]
  (let [config (config/read-config "config.edn")]
    (with-open [^HikariDataSource ds (create-connection-pool (:database config))]
      (.close (jdbc/get-connection ds))
      ; (jdbc/execute! ds ["select * from swift_ticketing.event"])
      (worker/workers ds 5)
      (run-jetty (app/swift-ticketing-app ds) {:port (get-in config [:server :port])
                                               :join? true})
      )))
