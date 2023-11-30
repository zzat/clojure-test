(ns swift-ticketing.migrations
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [swift-ticketing.config :as config]))

(defn make-connection-uri [db-config]
  (str "jdbc:postgresql://" (:host db-config) ":" (:port db-config)
       "/" (:dbname db-config) "?user=" (:username db-config)
       "&password=" (:password db-config)))

(defn migration-config [db-config]
  {:datastore  (jdbc/sql-database
                {:connection-uri (make-connection-uri db-config)})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate-with [db-config]
  (repl/migrate (migration-config db-config)))

(defn migrate []
  (migrate-with (:database (config/read-app-config))))

(defn rollback-with [db-config]
  (repl/rollback (migration-config db-config)))

(defn rollback []
  (rollback-with (:database (config/read-app-config))))

(comment (migrate-with (:database (config/read-test-config)))
         (rollback-with (:database (config/read-test-config))))
