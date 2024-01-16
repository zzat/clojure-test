(ns swift-ticketing.worker-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.core.async :as async]
            [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.worker :as worker]
            [swift-ticketing.factory :as factory]))

(use-fixtures :each fixtures/clear-tables)

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
              cancel-tickets-args (promise)
              update-booking-args (promise)]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/cancel-tickets
            (fn [_ ticket-ids]
              (deliver cancel-tickets-args {:ticket-ids ticket-ids}))
            db-booking/update-booking-status
            (fn [_ bid status]
              (deliver update-booking-args {:booking-status status
                                            :booking-id bid}))]

            (worker/request-ticket-cancellation
             message-queue
             {:booking-id booking-id})
            (worker/process-ticket-requests
             worker-id message-queue db-spec nil exit-ch)
            (is (= {:ticket-ids
                    (map :ticket-id selected-tickets)}
                   (deref cancel-tickets-args 2000 :timed-out)))
            (is (= {:booking-status db-booking/canceled
                    :booking-id booking-id}
                   (deref update-booking-args 2000 :timed-out)))
            (async/put! exit-ch :exit))))

      (testing "Book ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket))
                                    (range (inc (rand-int 20))))
              confirm-tickets-args (promise)
              update-booking-args (promise)]
          (with-redefs
           [db-ticket/lock-reserved-tickets (constantly selected-tickets)
            db-ticket/confirm-tickets
            (fn [_ ticket-ids]
              (deliver confirm-tickets-args {:ticket-ids ticket-ids}))
            db-booking/update-booking-status
            (fn [_ bid status]
              (deliver update-booking-args {:booking-status status
                                           :booking-id bid}))]

            (worker/request-ticket-booking
             message-queue
             {:booking-id booking-id})
            (worker/process-ticket-requests
             worker-id message-queue db-spec nil exit-ch)
            (is (= {:ticket-ids
                    (map :ticket-id selected-tickets)}
                   (deref confirm-tickets-args 2000 :timed-out)))
            (is (= {:booking-status db-booking/confirmed
                    :booking-id booking-id}
                   (deref update-booking-args 2000 :timed-out))))))

      (testing "Reserve ticket request"
        (let [booking-id (random-uuid)
              selected-tickets (map (constantly (factory/mk-ticket))
                                    (range (inc (rand-int 20))))
              reserve-tickets-args (atom (promise))
              update-booking-args (atom (promise))]

          (testing "with zero ticket quantity"
            (with-redefs
             [db-booking/update-booking-status
              (fn [_ bid status]
                (deliver @update-booking-args {:booking-status status
                                               :booking-id bid}))]

              (worker/request-ticket-reservation
               message-queue
               (factory/worker-reserve-ticket-request booking-id []))
              (worker/process-ticket-requests 1 message-queue db-spec nil exit-ch)
              (is (= {:booking-status db-booking/rejected
                      :booking-id booking-id}
                     (deref @update-booking-args 2000 :timed-out)))
              (async/put! exit-ch :exit)))

          (reset! update-booking-args (promise))

          (testing "with valid ticket ids"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly selected-tickets)
              db-ticket/reserve-tickets
              (fn [_ ticket-ids bid _]
                (deliver @reserve-tickets-args {:ticket-ids ticket-ids
                                                :booking-id bid}))
              db-booking/update-booking-status
              (fn [_ bid status]
                (deliver @update-booking-args {:booking-status status
                                               :booking-id bid}))]

              (worker/request-ticket-reservation
               message-queue
               (factory/worker-reserve-ticket-request
                booking-id selected-tickets))
              (worker/process-ticket-requests
               worker-id message-queue db-spec nil exit-ch)
              (is (= {:ticket-ids (map :ticket-id selected-tickets)
                      :booking-id booking-id}
                     (deref @reserve-tickets-args 2000 :timed-out)))
              (is (= {:booking-status db-booking/payment-pending
                      :booking-id booking-id}
                     (deref @update-booking-args 2000 :timed-out)))

              (async/put! exit-ch :exit)))

          (reset! reserve-tickets-args (promise))
          (reset! update-booking-args (promise))

          (testing "when unable to lock tickets"
            (with-redefs
             [db-ticket/lock-unbooked-tickets (constantly [])
              db-booking/update-booking-status
              (fn [_ bid status]
                (deliver @update-booking-args {:booking-status status
                                               :booking-id bid}))]

              (worker/request-ticket-reservation
               message-queue
               (factory/worker-reserve-ticket-request
                booking-id selected-tickets))
              (worker/process-ticket-requests
               worker-id message-queue db-spec nil exit-ch)
              (is (= :timed-out (deref @reserve-tickets-args 2000 :timed-out)))
              (is (= {:booking-status db-booking/rejected
                      :booking-id booking-id}
                     (deref @update-booking-args 2000 :timed-out)))
              (async/put! exit-ch :exit))))))))
