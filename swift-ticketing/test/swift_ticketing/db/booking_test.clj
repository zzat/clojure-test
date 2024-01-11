(ns swift-ticketing.db.booking-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.factory :as factory]))

(use-fixtures :each fixtures/clear-tables)

(deftest insert-booking-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        expected {:booking-id (random-uuid)
                  :user-id test-user-id
                  :booking-status (factory/random-booking-status)}]
    (testing "Insert booking query"
      (db-booking/insert-booking
       db-spec
       (:user-id expected)
       (:booking-id expected)
       (:booking-status expected))
      (let [booking (db-booking/get-booking db-spec (:booking-id expected))]
        (is (= expected
               (dissoc booking :created-at :updated-at)))))))

(deftest get-booking-status-test
  (testing "Get booking status query"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          booking {:booking-id (random-uuid)
                   :user-id test-user-id
                   :booking-status (factory/random-booking-status)}]
      (db-booking/insert-booking
       db-spec
       (:user-id booking)
       (:booking-id booking)
       (:booking-status booking))
      (is (= (:booking-status booking)
             (db-booking/get-booking-status db-spec (:booking-id booking)))))))

(deftest update-booking-status-test
  (testing "Update booking status query"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          booking {:booking-id (random-uuid)
                   :user-id test-user-id
                   :booking-status (factory/random-booking-status)}
          new-booking-status (factory/random-booking-status)]
      (db-booking/insert-booking
       db-spec
       (:user-id booking)
       (:booking-id booking)
       (:booking-status booking))
      (db-booking/update-booking-status
       db-spec
       (:booking-id booking)
       new-booking-status)
      (is (= new-booking-status
             (db-booking/get-booking-status db-spec (:booking-id booking)))))))
