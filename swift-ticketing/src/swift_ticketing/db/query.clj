(ns swift-ticketing.db.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn- run-query-with
  [f db-spec query]
   (f db-spec query {:builder-fn rs/as-unqualified-maps}))

(defn run-query!
  [db-spec query]
   (run-query-with jdbc/execute! db-spec query))

(defn run-query-one!
  [db-spec query]
   (run-query-with jdbc/execute-one! db-spec query))
