(ns swift-ticketing.handlers-test
  (:require [clojure.test :refer [deftest use-fixtures testing is]]
            [clojure.walk :refer [keywordize-keys]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.model.event :as event]
            [swift-ticketing.db.event :as db-event]
            [swift-ticketing.model.ticket :as ticket]
            [swift-ticketing.client :as client]
            [swift-ticketing.model.booking :as booking]))

(use-fixtures :each fixtures/clear-tables)

(deftest list-events-handler-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        event-req (keywordize-keys (factory/event-request))]

    (testing "List events handler"
      (testing "with valid request"
        (event/create-event db-spec test-user-id event-req)
        (let [{:keys [response status]} (client/list-events)
              event-response-keys ["event_id"
                                   "event_name"
                                   "event_description"
                                   "event_date"
                                   "venue"]
              keys-present? #(every?
                              (partial contains? %)
                              event-response-keys)]
          (is (= status 200))
          (is (vector? response))
          (is (every? keys-present? response))

          (testing "with query params"
            (let [venue (:venue event-req)
                  date (:date event-req)
                  valid-venue-resp (client/list-events {:venue venue})
                  non-existent-venue-resp (client/list-events
                                           {"venue" "Some non existant venue"})
                  valid-from-resp (client/list-events
                                   {"from" date})
                  invalid-from-resp (client/list-events
                                     {"from" "Invalid Date"})
                  valid-to-resp (client/list-events
                                 {"to" date})
                  invalid-to-resp (client/list-events
                                   {"to" "Invalid Date"})
                  valid-responses [valid-venue-resp
                                   valid-to-resp
                                   valid-from-resp]
                  invalid-responses [invalid-to-resp
                                     invalid-from-resp]]

              (testing "(valid params)"
                (is (every? #(= (:status %) 200) valid-responses))
                (is (every? #(every? keys-present? %) (map :response valid-responses))
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

(deftest create-event-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        create-req (factory/event-request)
        create-event-called-with-correct-params (atom false)
        expected-event-id :some-event-id]
    (testing "Create event"
      (testing "with valid request"
        (with-redefs
         [event/create-event
          (fn [dbs uid req]
            (when (and
                   (= db-spec dbs)
                   (= test-user-id
                      (java.util.UUID/fromString uid))
                   (= (keywordize-keys create-req) req))
              (reset! create-event-called-with-correct-params true))
            expected-event-id)]
          (let [{:keys [request status response]} (client/create-event create-req)
                event-id (get response "event_id")]
            (is (= status 201))
            (is (= (name expected-event-id) event-id) "Should return event_id returned by event/create-event")
            (is @create-event-called-with-correct-params)))))

    (testing "with missing params in request"
      (let [event (factory/event-request)]
        (doseq [key (keys event)]
          (let [request (dissoc event key)
                {:keys [request status response]} (client/create-event request)]
            (is (= status 400)
                (str "Request without '" key "' should return 400"))))))))

(defn create-ticket-test* [event-id ticket-request-fn]
  (testing "with valid request"
    (let [{:keys [test-user-id db-spec]} fixtures/test-env
          ticket-request (ticket-request-fn)
          ticket-type-id (str (java.util.UUID/randomUUID))
          stubbed-tickets [{:ticket-id (java.util.UUID/randomUUID)}]
          create-tickets-called-with-correct-args (atom false)]
      (with-redefs
       [ticket/create-tickets
        (fn [dbs uid eid req]
          (when (and
                 (= db-spec dbs)
                 (= test-user-id
                    (java.util.UUID/fromString uid))
                 (= (keywordize-keys ticket-request) req))
            (reset! create-tickets-called-with-correct-args true))
          {:ticket-type-id ticket-type-id
           :tickets stubbed-tickets})]
        (let [{:keys [response status]} (client/create-tickets event-id ticket-request)
              tickets (get response "tickets")]
          (is (= status 201))
          (is (= ticket-type-id (get response "ticket_type_id")))
          (is (contains? response "tickets"))
          (is (every? #(contains? % "ticket_id") tickets))

          (testing "with missing keys in request body"
            (doseq [key (keys ticket-request)]
              (let [ticket-req (dissoc ticket-request key)
                    {:keys [request response status]} (client/create-tickets event-id ticket-req)]
                (is (= status 400)
                    (str "Request without '" key "' should return 400"))))))))))

(deftest create-ticket-test
  (let [event-id (java.util.UUID/randomUUID)]
    (testing "Creating ticket (General)"
      (create-ticket-test* event-id factory/general-ticket-request))
    (testing "Creating ticket (Seated)"
      (create-ticket-test* event-id factory/seated-ticket-request))))

(defn- reserve-ticket-test* [reserve-ticket-req-fn]
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        expected-booking-id (str (java.util.UUID/randomUUID))
        event-id (java.util.UUID/randomUUID)
        reserve-ticket-req (reserve-ticket-req-fn)
        reserve-ticket-called-with-correct-args (atom false)]
    (testing "with valid request"
      (with-redefs [ticket/reserve-ticket
                    (fn [dbs _ uid eid req]
                      (when (and
                             (= db-spec dbs)
                             (= test-user-id
                                (java.util.UUID/fromString uid))
                             (= (keywordize-keys reserve-ticket-req) req))
                        (reset! reserve-ticket-called-with-correct-args true))
                      expected-booking-id)]
        (let [{:keys [status response]}
              (client/reserve-ticket event-id reserve-ticket-req)
              booking-id (get response "booking_id")]
          (is (= status 201))
          (is (= expected-booking-id booking-id)))))

    (testing "with missing keys in request body"
      (doseq [key (keys reserve-ticket-req)]
        (let [reserve-ticket-req* (dissoc reserve-ticket-req key)
              {:keys [status]}
              (client/reserve-ticket event-id reserve-ticket-req*)]
          (is (= status 400)
              (str "Request without '" key "' should return 400")))))))

(deftest reserve-ticket-test
  (let [event-id (java.util.UUID/randomUUID)]
    (testing "Reserving ticket (General)"
      (reserve-ticket-test* #(factory/mk-reserve-general-ticket-request
                              (inc (rand-int 10))
                              (java.util.UUID/randomUUID)))
      (testing "Reserving ticket (Seated)"
        (reserve-ticket-test* #(factory/mk-reserve-seated-ticket-request
                                [(java.util.UUID/randomUUID)]))))))

(deftest make-payment-test
  (testing "Payment handler"
    (let [booking-id (str (java.util.UUID/randomUUID))
          make-payment-called-with-correct-args (atom false)]
      (with-redefs [booking/make-payment
                    (fn [_ bid]
                      (when (= booking-id bid)
                        (reset! make-payment-called-with-correct-args true)))]
        (let [{:keys [status response]} (client/make-payment booking-id)]
          (is (= status 200))
          (is @make-payment-called-with-correct-args)
          (is (= booking-id (get response "booking_id"))))))))

(deftest cancel-booking-test
  (testing "Cancel Booking handler"
    (let [booking-id (str (java.util.UUID/randomUUID))
          cancel-booking-called-with-correct-args (atom false)]
      (with-redefs [booking/cancel-booking
                    (fn [_ bid]
                      (when (= booking-id bid)
                        (reset! cancel-booking-called-with-correct-args true)))]
        (let [{:keys [status response]} (client/cancel-booking booking-id)]
          (is (= status 200))
          (is @cancel-booking-called-with-correct-args)
          (is (= booking-id (get response "booking_id"))))))))

(deftest get-booking-status-test
  (testing "Get Booking Status handler"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (str (java.util.UUID/randomUUID))
          expected-booking-status :booking-status
          get-booking-status-called-with-correct-args (atom false)]
      (with-redefs [booking/get-booking-status
                    (fn [dbs bid]
                      (when (and
                             (= db-spec dbs)
                             (= booking-id bid))
                        (reset! get-booking-status-called-with-correct-args true))
                      expected-booking-status)]
        (let [{:keys [status response]} (client/get-booking-status booking-id)]
          (is (= status 200))
          (is @get-booking-status-called-with-correct-args)
          (is (= (name expected-booking-status) (get response "booking_status"))))))))

(deftest get-tickets-test
  (testing "Get tickets by ticket type"
    (let [{:keys [db-spec]} fixtures/test-env
          ticket-type-id (str (java.util.UUID/randomUUID))
          stubbed-tickets [:tickets]]
      (with-redefs [ticket/get-tickets
                    (fn [dbs tid]
                      (when (and (= db-spec dbs)
                                 (= ticket-type-id tid)))
                      stubbed-tickets)]
        (let [{:keys [status response]} (client/get-tickets ticket-type-id)]
          (is (= status 200))
          (is (= (map name stubbed-tickets) response)))))))

(deftest get-tickets-by-booking-id-test
  (testing "Get tickets by booking id"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (str (java.util.UUID/randomUUID))
          stubbed-tickets [:tickets]]
      (with-redefs [ticket/get-tickets-by-booking-id
                    (fn [dbs bid]
                      (when (and (= db-spec dbs)
                                 (= booking-id bid)))
                      stubbed-tickets)]
        (let [{:keys [status response]} (client/get-tickets-by-booking-id booking-id)]
          (is (= status 200))
          (is (= (map name stubbed-tickets) response)))))))
