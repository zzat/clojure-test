(ns swift-ticketing.handlers-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]
            [next.jdbc.connection :as connection]
            [clojure.data.json :as json]
            [clojure.set :as s]
            [swift-ticketing.app :refer [swift-ticketing-app]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.utils :as utils]
            [swift-ticketing.db.event :as event]))

(use-fixtures :once fixtures/fixture)

(deftest test-app
  (testing "Swift ticketing app:"
    (let [test-user-id (java.util.UUID/randomUUID)
          db-spec (:db-spec fixtures/test-env)
          app (fn [req] ((swift-ticketing-app db-spec) req))]

      (testing "Creating event"
        (testing "with valid request"
          (let [request (factory/event-request)
                response (-> (mock/request :post "/event")
                             (mock/json-body request)
                             (mock/cookie "uid" test-user-id)
                             app)
                response-json (-> response
                                  :body
                                  json/read-str)
                event-id (-> response-json
                             (get "event_id"))
                to-db-event (s/rename-keys request {"name" "event_name"
                                                    "description" "event_description"
                                                    "date" "event_date"})
                created-event (jdbc/execute!
                               db-spec
                               (event/get-event event-id))]
            (is (= (:status response) 201))
            (is ((comp not nil?) event-id) "Should return an event_id")
            (is (utils/submap? (to-db-event request) created-event)
                "Created data should match the data in request")))

        (testing "with missing params in request"
          (let [event (factory/event-request)]
            (doseq [key (keys event)]
              (let [request (dissoc event key)
                    response (-> (mock/request :post "/event")
                                 (mock/json-body request)
                                 (mock/cookie "uid" test-user-id)
                                 app)]
                (is (= (:status response) 400)
                    (str "Request without '" key "' should return 400"))))))))))

; (run-test test-app)
