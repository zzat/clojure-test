(ns swift-ticketing.booking-flow-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.client :as client]
            [swift-ticketing.db.booking :as db-booking]
            [camel-snake-kebab.extras :as cske]
            [camel-snake-kebab.core :as csk]))

(use-fixtures :each fixtures/clear-tables)

(deftest general-ticket-booking-flow-test
  (testing "Ticket booking (General)"
    ;; setup event and tickets
    (let [{:keys [db-spec]} fixtures/test-env
          event-id (->> (client/create-event)
                        :response
                        (cske/transform-keys csk/->kebab-case-keyword)
                        :event-id)
          {:keys [ticket-type-id
                  tickets]} (->> (client/create-general-tickets event-id)
                                 :response
                                 (cske/transform-keys csk/->kebab-case-keyword))
          total-tickets (count tickets)
          reserve-tickets (fn [quantity]
                            (->> (factory/mk-reserve-general-ticket-request
                                  quantity
                                  ticket-type-id)
                                 (client/reserve-ticket event-id)))
          get-booking-id #(get % "booking_id")]

      (testing "Reserving more tickets than available"
        ;; Reserving more tickets than available should return REJECTED
        (let [{:keys [status response]} (reserve-tickets (inc total-tickets))
              booking-id (get-booking-id response)]
          (is (= 201 status))
          (Thread/sleep 2000)
          (is (= db-booking/rejected
                 (db-booking/get-booking-status db-spec booking-id)))))

      (testing "Reserving available tickets"
        ;; Reserve available tickets -> PaymentPending
        (let [{:keys [status response]} (reserve-tickets total-tickets)
              booking-id (get-booking-id response)]
          (is (= 201 status))
          (Thread/sleep 2000)
          (is (= db-booking/payment-pending
                 (db-booking/get-booking-status db-spec booking-id)))

          (testing "Cancelling reserved ticket"
            (client/cancel-booking booking-id)
            (Thread/sleep 2000)
            (is (= db-booking/canceled
                   (db-booking/get-booking-status db-spec booking-id))))))

      (testing "Making Payment"
        (let [booking-id (-> (reserve-tickets total-tickets)
                             :response
                             (get "booking_id"))]
          (Thread/sleep 2000)
          (client/make-payment booking-id)
          (Thread/sleep 2000)
          (is (= db-booking/confirmed
                 (db-booking/get-booking-status db-spec booking-id))))))))

(deftest seated-ticket-booking-flow-test
  (testing "Ticket booking (Seated)"
    (let [{:keys [db-spec]} fixtures/test-env
          event-id (->> (client/create-event)
                        :response
                        (cske/transform-keys csk/->kebab-case-keyword)
                        :event-id)
          tickets (->> (client/create-seated-tickets event-id)
                       :response
                       (cske/transform-keys csk/->kebab-case-keyword)
                       :tickets)
          ticket-ids (map :ticket-id tickets)
          reserve-tickets (fn [tids]
                            (->> (factory/mk-reserve-seated-ticket-request
                                  tids)
                                 (client/reserve-ticket event-id)))
          get-booking-id #(get % "booking_id")]

      (testing "Reserving available tickets"
        (let [{:keys [status response]} (reserve-tickets ticket-ids)
              booking-id (get-booking-id response)]
          (is (= 201 status))
          (Thread/sleep 2000)
          (is (= db-booking/payment-pending
                 (db-booking/get-booking-status db-spec booking-id)))

          (testing "Cancelling reserved ticket"
            (client/cancel-booking booking-id)
            (Thread/sleep 2000)
            (is (= db-booking/canceled
                   (db-booking/get-booking-status db-spec booking-id))))))

      (testing "Making Payment"
        (let [booking-id (-> (reserve-tickets ticket-ids)
                             :response
                             (get "booking_id"))]
          (Thread/sleep 2000)
          (client/make-payment booking-id)
          (Thread/sleep 2000)
          (is (= db-booking/confirmed
                 (db-booking/get-booking-status db-spec booking-id))))))))
