(ns swift-ticketing.swift-ticketing-app-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [clojure.data.json :as json]
            [swift-ticketing.app :refer [swift-ticketing-app]]
            [swift-ticketing.db.ddl :as ddl]
            [swift-ticketing.core :refer [create-connection-pool]]
            [swift-ticketing.config :refer [read-config]])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn init-db [schema db-spec]
  (doseq [query [ddl/create-user-table
                 ddl/create-type-booking-status
                 ddl/create-event-table
                 ddl/create-booking-table
                 ddl/create-ticket-table]]
    (jdbc/execute! db-spec query)))

(defn teardown-db [schema db-spec]
  (jdbc/execute! db-spec [(ddl/delete-schema schema)]))

(deftest test-app
  (testing "Swift ticketing app"
    (let [db-config (:database (read-config "config.test.edn"))]
      (let [datasource (jdbc/get-datasource (assoc db-config :user (:username db-config)))
            ds (jdbc/get-connection datasource)]
        (jdbc/execute! ds [(ddl/create-schema (:schema db-config))]))
      (with-open [^HikariDataSource db-spec (create-connection-pool db-config)]
        (init-db (:schema db-config) db-spec)
        (testing "GET event route"
          (let [response ((swift-ticketing-app db-spec) (mock/request :get "/event"))]
            (is (= (:status response) 200))
            (is (vector? (json/read-str (:body response))))))

        (testing "POST event route"
          (let [response ((swift-ticketing-app db-spec)
                          (-> (mock/request :post "/event")
                              (mock/json-body
                               {"name" "Mock Event"
                                "description" "Mock Event description"
                                "date" "2024-10-01"
                                "venue" "Bangalore"})
                              (mock/cookie "uid" "e0119c9d-67b0-4eef-a5f1-a9463f070c57")))]
            (is (= (:status response) 201))
            (is (contains? (json/read-str (:body response)) "event_id"))))

        (testing "not-found route"
          (let [response ((swift-ticketing-app db-spec) (mock/request :get "/invalid"))]
            (is (= (:status response) 404))))
        (teardown-db (:schema db-config) db-spec)))))
