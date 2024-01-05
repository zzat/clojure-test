(ns swift-ticketing.model.booking-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :as async]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.worker :as worker]
            [swift-ticketing.model.booking :as booking]
            [swift-ticketing.db.booking :as db-booking]))

(deftest get-booking-status-test
  (testing "Fetching Booking Status"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (random-uuid)
          booking-status (factory/random-booking-status)
          get-booking-status-args (atom {})]
      (with-redefs
       [db-booking/get-booking-status
        (fn [dbs bid]
          (reset! get-booking-status-args
                  {:db-spec dbs
                   :booking-id bid})
          booking-status)]
        (is (= booking-status
               (booking/get-booking-status db-spec booking-id)))
        (is (= {:db-spec db-spec
                :booking-id booking-id}
               @get-booking-status-args))))))

(deftest make-payment-test
  (testing "Payment"
    (let [message-queue (async/chan)
          booking-id (random-uuid)
          add-book-ticket-request-to-queue-args (atom {})]
      (with-redefs
       [worker/add-book-ticket-request-to-queue
        (fn [mq req]
          (reset! add-book-ticket-request-to-queue-args
                  {:message-queue mq
                   :booking-request req}))]
        (booking/make-payment message-queue booking-id)
        (is (= {:message-queue message-queue
                :booking-request {:booking-id booking-id}}
               @add-book-ticket-request-to-queue-args))))))

(deftest cancel-booking-test
  (testing "Ticket Cancellation"
    (let [message-queue (async/chan)
          booking-id (random-uuid)
          add-cancel-ticket-request-to-queue-args (atom {})]
      (with-redefs
       [worker/add-cancel-ticket-request-to-queue
        (fn [mq req]
          (reset! add-cancel-ticket-request-to-queue-args
                  {:message-queue mq
                   :cancellation-request req}))]
        (booking/cancel-booking message-queue booking-id)
        (is (= {:message-queue message-queue
                :cancellation-request {:booking-id booking-id}}
               @add-cancel-ticket-request-to-queue-args))))))
