(ns swift-ticketing.db.booking-test
  (:require [clojure.test :refer :all]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.db.query :as query]))

(use-fixtures :once fixtures/setup-test-system)
(use-fixtures :each fixtures/clear-tables)

(deftest insert-booking-test
  (let [{:keys [test-user-id]} fixtures/test-env
        expected {:booking_id (java.util.UUID/randomUUID)
                  :user_id test-user-id
                  :booking_status db-booking/INPROCESS}]
    (testing "Insert booking query"
      (query/run-query! (db-booking/insert-booking 
                          (:user_id expected) 
                          (:booking_id expected)))
      (let [booking (query/get-booking (:booking_id expected))]
        (is (= (dissoc booking :created_at :updated_at) expected))))))

(deftest get-booking-status-test
  (testing "Get booking status query"
    (let [{:keys [test-user-id]} fixtures/test-env
          booking {:booking_id (java.util.UUID/randomUUID)
                   :user_id test-user-id
                   :booking_status db-booking/INPROCESS}]
      (query/insert-booking booking)
      (is (= (query/get-booking-status (:booking_id booking))
             (:booking_status booking))))))

(deftest update-booking-status-test
  (testing "Update booking status query"
    (let [{:keys [test-user-id]} fixtures/test-env
          booking {:booking_id (java.util.UUID/randomUUID)
                   :user_id test-user-id
                   :booking_status db-booking/INPROCESS}
          new-booking-status db-booking/CONFIRMED]
      (query/insert-booking booking)
      (query/run-query! (db-booking/update-booking-status 
                          (:booking_id booking) 
                          new-booking-status))
      (is (= (query/get-booking-status (:booking_id booking))
             new-booking-status)))))

; (run-test insert-booking-test)
; (run-test get-booking-status-test)
; (run-test update-booking-status-test)
