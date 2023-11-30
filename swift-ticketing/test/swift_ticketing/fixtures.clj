(ns swift-ticketing.fixtures
  (:require
   [next.jdbc :as jdbc]
   [honey.sql :as sql]
   [swift-ticketing.core :refer [create-connection-pool]]
   [swift-ticketing.db.ddl :as ddl]
   [swift-ticketing.config :refer [read-test-config]]
   [swift-ticketing.migrations :as migrations]))

(def test-env 
  (let [db-config (:database (read-test-config))]
    {:db-spec (create-connection-pool db-config)})
  )

(defn fixture [tests]
 (migrations/migrate-with (:database (read-test-config))) 
 (tests)
 (migrations/rollback-with (:database (read-test-config))) 
)
