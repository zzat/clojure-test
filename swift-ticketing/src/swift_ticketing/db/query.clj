(ns swift-ticketing.db.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn run-query!
  [db-spec query]
   (jdbc/execute! db-spec query {:builder-fn rs/as-unqualified-maps}))
