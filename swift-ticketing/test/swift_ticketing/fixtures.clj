(ns swift-ticketing.fixtures
  (:require
   [next.jdbc :as jdbc]
   [honey.sql :as sql]
   [swift-ticketing.core :refer [create-connection-pool]]
   [swift-ticketing.factory :as factory]
   [swift-ticketing.config :refer [read-test-config]]
   [swift-ticketing.migrations :as migrations]))

(def test-env
  (let [db-config (:database (read-test-config))]
    {:db-spec (create-connection-pool db-config)}))

(defn run-migrations [tests]
  (let [db-config (:database (read-test-config))]
    (migrations/migrate-with db-config)
    (let [test-user-id (factory/add-user-table-entry (:db-spec test-env))] 
      (alter-var-root #'test-env #(assoc % :test-user-id test-user-id)))
    (tests)
    (migrations/rollback-with db-config)))