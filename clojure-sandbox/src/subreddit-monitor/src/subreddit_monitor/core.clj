(ns subreddit-monitor.core
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [subreddit-monitor.reddit.service :as service])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn create-connection-pool [jdbcUrl]
  (connection/->pool com.zaxxer.hikari.HikariDataSource
                     {:jdbcUrl jdbcUrl}))

(defn -main
  [& args]
  (println "Starting...")
  (let [config {:jdbcUrl "jdbc:sqlite:/Users/zzat/Documents/sqlite-db"}]
    (with-open [^HikariDataSource ds (create-connection-pool (:jdbcUrl config))]
      (.close (jdbc/get-connection ds))
      ; (service/init-db ds)
      (repeatedly
       #(do
          (service/scrape-subreddit ds "Clojure")
          (service/print-stats ds "Clojure")
          (Thread/sleep 10000)))
      ; (fetch-reddit-data!)
      )))
