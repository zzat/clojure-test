(ns swift-ticketing.swift-ticketing-app-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
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

(defn add-user-table-entry [db-spec]
  (let [user-id (java.util.UUID/randomUUID)
        insert-user (sql/format {:insert-into :user_account
                                 :columns [:user_id :name]
                                 :values [[user-id
                                           "Test User"]]})]
    (jdbc/execute! db-spec insert-user)
    user-id))

(defn setup-db [schema db-spec]
  (init-db schema db-spec)
  (add-user-table-entry db-spec))

(deftest test-app
  (testing "Swift ticketing app"
    (let [db-config (:database (read-config "config.test.edn"))]
      (let [datasource (jdbc/get-datasource (assoc db-config :user (:username db-config)))
            ds (jdbc/get-connection datasource)]
        (jdbc/execute! ds [(ddl/create-schema (:schema db-config))]))
      (with-open [^HikariDataSource db-spec (create-connection-pool db-config)]
        (let [test-user-id (setup-db (:schema db-config) db-spec)]

          (testing "GET event"
            (let [response ((swift-ticketing-app db-spec) (mock/request :get "/event"))]
              (is (= (:status response) 200))
              (is (vector? (json/read-str (:body response))))))

          (testing "POST event"
            (let [response ((swift-ticketing-app db-spec)
                            (-> (mock/request :post "/event")
                                (mock/json-body
                                 {"name" "Mock Event"
                                  "description" "Mock Event description"
                                  "date" "2024-10-01"
                                  "venue" "Bangalore"})
                                (mock/cookie "uid" test-user-id)))]
              (is (= (:status response) 201))
              (is (contains? (json/read-str (:body response)) "event_id"))))

          (testing "Create event and its tickets"
            (let [event-response ((swift-ticketing-app db-spec)
                                  (-> (mock/request :post "/event")
                                      (mock/json-body
                                       {"name" "Mock Event"
                                        "description" "Mock Event description"
                                        "date" "2024-10-01"
                                        "venue" "Bangalore"})
                                      (mock/cookie "uid" test-user-id)))]
              (testing "POST event"
                (is (= (:status event-response) 201))
                (is (contains? (json/read-str (:body event-response)) "event_id")))

              (testing "POST ticket"
                (let [created-event-id (get (json/read-str (:body event-response)) "event_id")
                      ticket-response (-> ((swift-ticketing-app db-spec)
                                           (-> (mock/request :post (str "/event/" created-event-id "/ticket"))
                                               (mock/json-body
                                                {"name" "Ticket 1"
                                                 "description" "Ticket 1"
                                                 "quantity" 2
                                                 "price" 250})
                                               (mock/cookie "uid" test-user-id)))
                                          :body
                                          json/read-str)]
                  ; (is (= (:status ticket-response) 201))
                  (is (contains? ticket-response "tickets"))

                  (testing "Ticket Booking"
                    (let [booking-response (-> ((swift-ticketing-app db-spec)
                                                (-> (mock/request :post (str "/event/" created-event-id "/booking"))
                                                    (mock/json-body
                                                     {"ticket_name" "Ticket 1"
                                                      "quantity" 1})
                                                    (mock/cookie "uid" test-user-id)))
                                               :body
                                               json/read-str)]
                      ; (is (= (:status booking-response) 201))
                      (is (contains? booking-response "booking_id"))))))))

          (testing "not-found route"
            (let [response ((swift-ticketing-app db-spec) (mock/request :get "/invalid"))]
              (is (= (:status response) 404))))

          (teardown-db (:schema db-config) db-spec)
          )))))
