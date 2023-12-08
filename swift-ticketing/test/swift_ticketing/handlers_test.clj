(ns swift-ticketing.handlers-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [next.jdbc :as jdbc]
            [clojure.data.json :as json]
            [next.jdbc.result-set :as rs]
            [clojure.set :as s]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [swift-ticketing.app :refer [swift-ticketing-app]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.utils :as utils]
            [swift-ticketing.db.event :as db-event]
            [swift-ticketing.db.ticket :as ticket]
            [swift-ticketing.client :as client]
            [swift-ticketing.specs :as specs]))

(use-fixtures :once fixtures/setup-test-system)
(use-fixtures :each fixtures/clear-tables)

(deftest create-event-test
  (let [{:keys [db-spec]} fixtures/test-env]

    (testing "Create event"
      (testing "with valid request"
        (let [{:keys [request status response]} (client/create-event)
              event-id (get response "event_id")
              created-event (first (db-event/get-event db-spec event-id))]
          (is (= status 201))
          (is ((comp not nil?) event-id) "Should return an event_id")
          (is (= request (utils/db-event-to-event-request created-event))
              "Created data should match the data in request")))

      (testing "with missing params in request"
        (let [event (factory/event-request)]
          (doseq [key (keys event)]
            (let [request (dissoc event key)
                  {:keys [request status response]} (client/create-event request)]
              (is (= status 400)
                  (str "Request without '" key "' should return 400")))))))))

(deftest list-events-test
  (let [{:keys [db-spec]} fixtures/test-env
        app (fn [req] ((swift-ticketing-app db-spec) req))]

    (testing "List events"
      (testing "with valid request"
        (client/create-event)
        (let [{:keys [response status]} (client/list-events)
              event-response-keys ["event_id"
                                   "event_name"
                                   "event_description"
                                   "event_date"
                                   "venue"]
              keys-present? #(every?
                              (partial contains? %)
                              event-response-keys)
              ; valid-date? #(instance? java.util.Date (get % "event_date"))
              ]
          (is (= status 200))
          (is (vector? response))
          (is (every? keys-present? response))

          (testing "with query params"
            (let [an-event (first response)
                  venue (get an-event "venue")
                  date (-> an-event
                           (get "event_date")
                           utils/format-json-date)
                  req-with-query-params (fn [qp] (-> (mock/request :get "/event")
                                                     (mock/query-string qp)
                                                     app))
                  valid-venue-resp (client/list-events {:venue venue})
                  non-existent-venue-resp (client/list-events
                                           {:venue "Some non existant venue"})
                  valid-from-resp (client/list-events
                                   {:from date})
                  invalid-from-resp (client/list-events
                                     {:from "Invalid Date"})
                  valid-to-resp (client/list-events
                                 {:to date})
                  invalid-to-resp (client/list-events
                                   {:to "Invalid Date"})
                  response-to-json (fn [r] (-> r
                                               :body
                                               json/read-str))
                  valid-responses [valid-venue-resp
                                   valid-to-resp
                                   valid-from-resp]
                  valid-responses-json (map :response valid-responses)
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
                (is (empty? (:response non-existent-venue-resp)))))))))))

(deftest get-event-test
  (testing "Get event with tickets info"
    (let [event-id (java.util.UUID/randomUUID)
          expected [(factory/event-with-tickets event-id)]]
      (with-redefs [db-event/get-event-with-tickets (constantly expected)]
        (let [{:keys [response status]} (client/get-event event-id)
              actual (-> response
                         keywordize-keys)]
          (is (= status 200))
          (is (= actual expected)))
        nil))))

(defn create-ticket-test* [event-id ticket-request-fn]
  (testing "with valid request"
    (let [{:keys [request response status]} (client/create-tickets event-id (ticket-request-fn))
          {:keys [db-spec test-user-id]} fixtures/test-env
          ticket-type-id (get response "ticket-type-id")
          created-tickets (jdbc/execute! db-spec (ticket/get-unbooked-tickets ticket-type-id) {:builder-fn rs/as-unqualified-maps})
          tickets (get response "tickets")
          get-ticket-ids (fn [t] (set (map #(get % "ticket_id") t)))
          ticket-ids (get-ticket-ids tickets)
          created-ticket-ids (get-ticket-ids created-tickets)]
      (is (= (:status response) 201))
      (is (contains? response "ticket_type_id"))
      (is (contains? response "tickets"))
      (is (every? #(contains? % "ticket-id") tickets))
      (is (= (count created-tickets) (count tickets)))
      (is (= created-ticket-ids ticket-ids))

      (testing "with missing keys in request body"
        (let [ticket-req (ticket-request-fn)]
          (doseq [key (keys ticket-req)]
            (let [ticket-req* (dissoc ticket-req key)
                  {:keys [request response status]} (client/create-tickets event-id ticket-req*)]
              (is (= status 400)
                  (str "Request without '" key "' should return 400")))))))))

(deftest create-ticket-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        app (fn [req] ((swift-ticketing-app db-spec) req))
        event-id (java.util.UUID/randomUUID)]
    (db-event/insert-event
     db-spec
     test-user-id
     event-id
     (keywordize-keys (factory/event-request)))
    (testing "Creating ticket (General)"
      (create-ticket-test* event-id factory/general-ticket-request))
    (testing "Creating ticket (Seated)"
      (create-ticket-test* event-id factory/seated-ticket-request))))

; (run-tests)
(run-test create-event-test)
(run-test list-events-test)
(run-test get-event-test)
