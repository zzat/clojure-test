(ns swift-ticketing.model.booking-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.core.async :as async]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.worker :as worker]
            [swift-ticketing.model.booking :as booking]
            [swift-ticketing.db.booking :as db-booking]))

(deftest get-booking-status-test
  (testing "Fetching Booking Status"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (random-uuid)]
      (with-redefs [db-booking/get-booking-status
                    (fn [dbs bid]
                      (when (and (= db-spec dbs)
                                 (= booking-id bid))
                        :ok))]
        (is (= :ok (booking/get-booking-status db-spec booking-id)))))))

(deftest make-payment-test
  (testing "Payment"
    (let [message-queue (async/chan)
          booking-id (random-uuid)]
      (with-redefs [worker/add-book-ticket-request-to-queue
                    (fn [mq req]
                      (when (and (= message-queue mq)
                                 (= booking-id (:booking-id req)))
                        :ok))]
        (is (= :ok (booking/make-payment message-queue booking-id)))))))

(deftest cancel-booking-test
  (testing "Ticket Cancellation"
    (let [message-queue (async/chan)
          booking-id (random-uuid)]
      (with-redefs [worker/add-cancel-ticket-request-to-queue
                    (fn [mq req]
                      (when (and (= message-queue mq)
                                 (= booking-id (:booking-id req)))
                        :ok))]
        (is (= :ok (booking/cancel-booking message-queue booking-id)))))))
