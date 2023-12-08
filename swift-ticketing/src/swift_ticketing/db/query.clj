(ns swift-ticketing.db.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defn run-query
  ([db-spec query-fn args opts]
   (jdbc/execute! db-spec (apply query-fn args) opts))
  ([db-spec query-fn args]
   (run-query db-spec query-fn args {:builder-fn rs/as-unqualified-maps})))
