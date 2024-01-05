(ns swift-ticketing.worker-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async]
            [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.worker :as worker]
            [swift-ticketing.factory :as factory]))

(use-fixtures :each fixtures/clear-tables)

(deftest add-reserve-ticket-request-to-queue-test
  (testing "Adding reservation request to queue"
    (let [message-queue (async/chan)
          request (factory/worker-reserve-ticket-request)]
      (worker/add-reserve-ticket-request-to-queue message-queue request)
      (is (= (async/<!! message-queue)
             {:event worker/RESERVE
              :data request})))))

(deftest add-book-ticket-request-to-queue-test
  (testing "Adding booking request to queue"
    (let [message-queue (async/chan)
          request {:booking-id (random-uuid)}]
      (worker/add-book-ticket-request-to-queue message-queue request)
      (is (= (async/<!! message-queue)
             {:event worker/BOOK
              :data request})))))

(deftest add-cancel-ticket-request-to-queue-test
  (testing "Adding cancel request to queue"
    (let [message-queue (async/chan)
          request {:booking-id (random-uuid)}]
      (worker/add-cancel-ticket-request-to-queue message-queue request)
      (is (= (async/<!! message-queue)
             {:event worker/CANCEL
              :data request})))))

(deftest process-ticket-requests-test
  (testing "Processing ticket requests"
    (let [{:keys [db-spec]} fixtures/test-env
          message-queue (async/chan)
          exit-ch (async/chan)
          worker-id (rand-int 10)]

      (testing "Cancel ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket))
                                    (range (inc (rand-int 20))))
              cancel-tickets-args (atom {})
              update-booking-args (atom {})]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/cancel-tickets
            (fn [_ ticket-ids]
              (reset! cancel-tickets-args {:ticket-ids ticket-ids}))
            db-booking/update-booking-status
            (fn [_ bid status]
              (reset! update-booking-args {:booking-status status
                                           :booking-id bid}))]

            (worker/add-cancel-ticket-request-to-queue
             message-queue
             {:booking-id booking-id})
            (worker/process-ticket-requests
             worker-id message-queue db-spec nil exit-ch)
            (Thread/sleep 2000)
            (is (= {:ticket-ids
                    (map :ticket-id selected-tickets)}
                   @cancel-tickets-args))
            (is (= {:booking-status db-booking/CANCELED
                    :booking-id booking-id}
                   @update-booking-args)))))

      (testing "Book ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket))
                                    (range (inc (rand-int 20))))
              confirm-tickets-args (atom {})
              update-booking-args (atom {})]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/confirm-tickets
            (fn [_ ticket-ids]
              (reset! confirm-tickets-args {:ticket-ids ticket-ids}))
            db-booking/update-booking-status
            (fn [_ bid status]
              (reset! update-booking-args {:booking-status status
                                           :booking-id bid}))]

            (worker/add-book-ticket-request-to-queue
             message-queue
             {:booking-id booking-id})
            (worker/process-ticket-requests
             worker-id message-queue db-spec nil exit-ch)
            (Thread/sleep 2000)
            (is (= {:ticket-ids
                    (map :ticket-id selected-tickets)}
                   @confirm-tickets-args))
            (is (= {:booking-status db-booking/CONFIRMED
                    :booking-id booking-id}
                   @update-booking-args)))))

      (testing "Reserve ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket))
                                    (range (inc (rand-int 20))))
              reserve-tickets-args (atom {})
              update-booking-args (atom {})]

          (testing "with zero ticket quantity"
            (with-redefs
             [db-booking/update-booking-status
              (fn [_ bid status]
                (reset! update-booking-args {:booking-status status
                                             :booking-id bid}))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request booking-id []))
              (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is (= {:booking-status db-booking/REJECTED
                      :booking-id booking-id}
                     @update-booking-args))))

          (reset! update-booking-args {})

          (testing "with valid ticket ids"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly selected-tickets)
              db-ticket/reserve-tickets
              (fn [_ ticket-ids bid _]
                (reset! reserve-tickets-args {:ticket-ids ticket-ids
                                              :booking-id bid}))
              db-booking/update-booking-status
              (fn [_ bid status]
                (reset! update-booking-args {:booking-status status
                                             :booking-id bid}))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request
                booking-id selected-tickets))
              (worker/process-ticket-requests
               worker-id message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is (= {:ticket-ids (map :ticket-id selected-tickets)
                      :booking-id booking-id}
                     @reserve-tickets-args))
              (is (= {:booking-status db-booking/PAYMENTPENDING
                      :booking-id booking-id}
                     @update-booking-args))))

          (reset! reserve-tickets-args {})
          (reset! update-booking-args {})

          (testing "when unable to lock tickets"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly [])
              db-booking/update-booking-status
              (fn [_ bid status]
                (reset! update-booking-args {:booking-status status
                                             :booking-id bid}))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request
                booking-id selected-tickets))
              (worker/process-ticket-requests
               worker-id message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is (= {} @reserve-tickets-args))
              (is (= {:booking-status db-booking/REJECTED
                      :booking-id booking-id}
                     @update-booking-args)))))))))
