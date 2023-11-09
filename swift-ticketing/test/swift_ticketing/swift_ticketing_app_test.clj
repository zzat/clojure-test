(ns swift-ticketing.swift-ticketing-app-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [swift-ticketing.app :refer :all]
            [swift-ticketing.core :refer [create-connection-pool]]
            [swift-ticketing.config :refer [read-config]]))

(def db-spec (jdbc/get-datasource (:database (read-config "config.edn"))))

(deftest test-app
  (testing "GET event route"
    (let [response ((swift-ticketing-app db-spec) (mock/request :get "/event"))]
      (is (= (:status response) 200))
      (is (vector? (:body response)))
      ))

  (testing "POST event route"
    (let [response ((swift-ticketing-app db-spec)
                    (-> (mock/request :post "/event")
                        (mock/json-body
                         {"name" "Mock Event"
                          "description" "Mock Event description"
                          "date" "2024-10-01"
                          "venue" "Bangalore"})))]
      (is (= (:status response) 201))
      (is (contains? (:body response) "event_id"))))

  (testing "not-found route"
    (let [response ((swift-ticketing-app db-spec) (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
