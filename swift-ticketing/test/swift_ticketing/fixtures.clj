(ns swift-ticketing.fixtures
  (:require
   [com.stuartsierra.component :as component]
   [swift-ticketing.core :refer [swift-ticketing-system]]
   [swift-ticketing.factory :as factory]
   [swift-ticketing.config :refer [read-test-config]]
   [swift-ticketing.migrations :as migrations]
   [next.jdbc :as jdbc]
   [honey.sql :as sql]))

(def test-env
  {:test-user-id nil
   :db-spec nil
   :server-spec nil})

(def swift-ticketing-test-system
  (swift-ticketing-system (read-test-config)))

(defn start-test-system []
  (alter-var-root #'swift-ticketing-test-system component/start))

(defn stop-test-system []
  (alter-var-root #'swift-ticketing-test-system component/stop))

(defn run-with-test-system [tests]
  ;; Init db connection pool, start workers
  (start-test-system)
  (let [db (:database swift-ticketing-test-system)
        http-server (:app swift-ticketing-test-system)
        db-config (:db-config db)
        conn (:connection db)]
    ;; setup tables
    (migrations/migrate-with db-config)
    (let [test-user-id (factory/add-user-table-entry conn)]
      ;; add a user, update it in test env
      (alter-var-root
       #'test-env #(assoc %
                          :test-user-id test-user-id
                          :db-spec conn
                          :server-spec http-server)))
    ;; run tests
    (tests)
    ;; rollback db
    (migrations/rollback-with db-config)
    ;; stop db connections, workers
    (stop-test-system)))

(defn setup-test-system [test-plan]
  ;; Init db connection pool, start workers
  (start-test-system)
  (let [db (:database swift-ticketing-test-system)
        http-server (:app swift-ticketing-test-system)
        db-config (:db-config db)
        conn (:connection db)]
    ;; setup tables
    (migrations/migrate-with db-config)
    (let [test-user-id (factory/add-user-table-entry conn)]
      ;; add a user, update it in test env
      (alter-var-root
       #'test-env #(assoc %
                          :test-user-id test-user-id
                          :db-spec conn
                          :server-spec http-server))))
  test-plan)

(defn teardown-test-system [test-plan]
  (let [db (:database swift-ticketing-test-system)
        db-config (:db-config db)]
    ;; rollback db
    (migrations/rollback-with db-config)
    ;; stop db connections, workers
    (stop-test-system))
  test-plan)

(defn truncate-tables [db-spec]
  (let [tables [:ticket :ticket_type :booking :event]
        truncate-fn #(sql/format {:truncate [% [:raw "cascade"]]})]
    (doseq [table tables]
      (jdbc/execute! db-spec (truncate-fn table)))))

(defn clear-tables [tests]
  (truncate-tables (:db-spec test-env))
  (tests))
