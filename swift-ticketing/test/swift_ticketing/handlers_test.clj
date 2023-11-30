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
            [swift-ticketing.db.event :as event]
            [swift-ticketing.specs :as specs]))

(use-fixtures :once fixtures/fixture)

(deftest test-handlers
  (testing "HTTP handlers:"
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
                    (str "Request without '" key "' should return 400")))))))

      (testing "Fetching events"
        (testing "with valid request"
          (let [response (-> (mock/request :get "/event")
                             app)
                response-json (-> response
                                  :body
                                  json/read-str)
                event-response-keys ["event_id"
                                     "event_name"
                                     "event_description"
                                     "event_date"
                                     "venue"]
                keys-present? #(every?
                                (partial contains? %)
                                event-response-keys)
                valid-date? #(instance? java.util.Date (get % "event_date"))]
            (is (= (:status response) 200))
            (is (vector? response-json))
            (is (every? keys-present? response-json))

            (testing "with query params"
              (let [an-event (first response-json)
                    venue (get an-event "venue")
                    date (-> an-event
                             (get "event_date")
                             utils/format-json-date)
                    req-with-query-params (fn [qp] (-> (mock/request :get "/event")
                                                       (mock/query-string qp)
                                                       app))
                    valid-venue-resp (req-with-query-params
                                      {:venue venue})
                    non-existent-venue-resp (req-with-query-params
                                             {:venue "Some non existant venue"})
                    valid-from-resp (req-with-query-params
                                     {:from date})
                    invalid-from-resp (req-with-query-params
                                       {:from "Invalid Date"})
                    valid-to-resp (req-with-query-params
                                   {:to date})
                    invalid-to-resp (req-with-query-params
                                     {:to "Invalid Date"})
                    response-to-json (fn [r] (-> r
                                                 :body
                                                 json/read-str))
                    valid-responses [valid-venue-resp
                                     valid-to-resp
                                     valid-from-resp]
                    valid-responses-json (map response-to-json valid-responses)
                    invalid-responses [invalid-to-resp
                                       invalid-from-resp]]
                (testing "(valid params)"
                  (is (every? #(= (:status %) 200) valid-responses))
                  (is (every? #(vector? %) valid-responses-json)
                      "response should be a vector")
                  (is (every? #(every? keys-present? %) valid-responses-json)
                      "response should have the required keys"))
                (testing "(invalid params)"
                ;; invalid requests dhould return 400
                  (is (every? #(= (:status %) 400) invalid-responses))
                ;; check for empty response
                  (is (empty? (:body (response-to-json non-existent-venue-resp)))))))))))))

; (run-test test-handlers)
