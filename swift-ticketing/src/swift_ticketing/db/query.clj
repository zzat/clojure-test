(ns swift-ticketing.db.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(defn- run-query-with
  [f db-spec query]
  (cske/transform-keys csk/->kebab-case-keyword
                       (f db-spec query {:builder-fn rs/as-unqualified-maps})))

(defn run-query!
  [db-spec query]
  (run-query-with jdbc/execute! db-spec query))

(defn run-query-one!
  [db-spec query]
  (run-query-with jdbc/execute-one! db-spec query))
