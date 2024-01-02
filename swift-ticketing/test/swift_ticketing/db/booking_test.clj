(ns swift-ticketing.db.booking-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.factory :as factory]))

(use-fixtures :each fixtures/clear-tables)

(deftest insert-booking-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        expected {:booking_id (random-uuid)
                  :user_id test-user-id
                  :booking_status db-booking/INPROCESS}]
    (testing "Insert booking query"
      (db-booking/insert-booking
       db-spec
       (:user_id expected)
       (:booking_id expected)
       (:booking_status expected))
      (let [booking (db-booking/get-booking db-spec (:booking_id expected))]
        (is (= expected
               (dissoc booking :created_at :updated_at)))))))

(deftest get-booking-status-test
  (testing "Get booking status query"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          booking {:booking_id (random-uuid)
                   :user_id test-user-id
                   :booking_status db-booking/INPROCESS}]
      (db-booking/insert-booking
       db-spec
       (:user_id booking)
       (:booking_id booking)
       (:booking_status booking))
      (is (= (:booking_status booking)
             (db-booking/get-booking-status db-spec (:booking_id booking)))))))

(deftest update-booking-status-test
  (testing "Update booking status query"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          booking {:booking_id (random-uuid)
                   :user_id test-user-id
                   :booking_status (factory/random-booking-status)}
          new-booking-status (factory/random-booking-status)]
      (db-booking/insert-booking
       db-spec
       (:user_id booking)
       (:booking_id booking)
       (:booking_status booking))
      (db-booking/update-booking-status
       db-spec
       (:booking_id booking)
       new-booking-status)
      (is (= new-booking-status
             (db-booking/get-booking-status db-spec (:booking_id booking)))))))
