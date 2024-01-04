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
          exit-ch (async/chan)]

      (testing "Cancel ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket)) 
                                    (range (inc (rand-int 20))))
              cancel-tickets-called-with-right-args (atom false)
              update-booking-called-with-right-args (atom false)]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/cancel-tickets
            (fn [_ ticket-ids]
              (when (= ticket-ids
                       (map :ticket_id selected-tickets))
                (reset! cancel-tickets-called-with-right-args true)))
            db-booking/update-booking-status
            (fn [_ bid status]
              (when (and
                     (= db-booking/CANCELED status)
                     (= booking-id bid))
                (reset! update-booking-called-with-right-args true)))]

            (worker/add-cancel-ticket-request-to-queue message-queue {:booking-id booking-id})
            (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
            (Thread/sleep 2000)
            (is @cancel-tickets-called-with-right-args)
            (is @update-booking-called-with-right-args))))

      (testing "Book ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket)) 
                                    (range (inc (rand-int 20))))
              confirm-tickets-called-with-right-args (atom false)
              update-booking-called-with-right-args (atom false)]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/confirm-tickets
            (fn [_ ticket-ids]
              (when (= ticket-ids
                       (map :ticket_id selected-tickets))
                (reset! confirm-tickets-called-with-right-args true)))
            db-booking/update-booking-status
            (fn [_ bid status]
              (when (and
                     (= db-booking/CONFIRMED status)
                     (= booking-id bid))
                (reset! update-booking-called-with-right-args true)))]

            (worker/add-book-ticket-request-to-queue message-queue {:booking-id booking-id})
            (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
            (Thread/sleep 2000)
            (is @confirm-tickets-called-with-right-args)
            (is @update-booking-called-with-right-args))))

      (testing "Reserve ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket)) 
                                    (range (inc (rand-int 20))))
              reserve-tickets-called-with-right-args (atom false)
              update-booking-called-with-right-args (atom false)]

          (testing "with zero ticket quantity"
            (with-redefs
             [db-booking/update-booking-status
              (fn [_ bid status]
                (when (and
                       (= db-booking/REJECTED status)
                       (= booking-id bid))
                  (reset! update-booking-called-with-right-args true)))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request booking-id []))
              (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is @update-booking-called-with-right-args)))

          (reset! update-booking-called-with-right-args false)

          (testing "with valid ticket ids"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly selected-tickets)
              db-ticket/reserve-tickets
              (fn [_ ticket-ids bid _]
                (when (and
                       (= booking-id bid)
                       (= ticket-ids
                          (map :ticket_id selected-tickets)))
                  (reset! reserve-tickets-called-with-right-args true)))
              db-booking/update-booking-status
              (fn [_ bid status]
                (when (and
                       (= db-booking/PAYMENTPENDING status)
                       (= booking-id bid))
                  (reset! update-booking-called-with-right-args true)))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request booking-id selected-tickets))
              (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is @reserve-tickets-called-with-right-args)
              (is @update-booking-called-with-right-args)))

          (reset! reserve-tickets-called-with-right-args false)
          (reset! update-booking-called-with-right-args false)

          (testing "when unable to lock tickets"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly [])
              db-booking/update-booking-status
              (fn [_ bid status]
                (when (and
                       (= db-booking/REJECTED status)
                       (= booking-id bid))
                  (reset! update-booking-called-with-right-args true)))]

              (worker/add-reserve-ticket-request-to-queue
               message-queue
               (factory/worker-reserve-ticket-request booking-id selected-tickets))
              (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
              (Thread/sleep 2000)
              (is (= false @reserve-tickets-called-with-right-args))
              (is @update-booking-called-with-right-args))))))))
