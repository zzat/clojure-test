(ns swift-ticketing.db.ticket-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.event :as db-event]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(use-fixtures :each fixtures/clear-tables)

(defn- create-event-and-ticket-type [ticket-request]
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        ticket-type-id (random-uuid)
        event-request (cske/transform-keys csk/->kebab-case-keyword
                                           (factory/event-request))
        event-id (random-uuid)]
    (db-event/insert-event db-spec test-user-id event-id event-request)
    (db-ticket/insert-ticket-type db-spec event-id ticket-type-id ticket-request)
    {:event-id event-id
     :ticket-type-id ticket-type-id}))

(deftest insert-ticket-type-test
  (testing "Inserting Ticket Type"
    (let [{:keys [db-spec]} fixtures/test-env
          ticket-request (cske/transform-keys csk/->kebab-case-keyword
                                              (factory/general-ticket-request))
          {:keys [event-id ticket-type-id]} (create-event-and-ticket-type ticket-request)]
      (let [expected {:ticket-type-id ticket-type-id
                      :ticket-type (:ticket_type ticket-request)
                      :ticket-type-description (:description ticket-request)
                      :event-id event-id
                      :reservation-timelimit-seconds nil
                      :seat-type (:seat_type ticket-request)}
            ticket-type (db-ticket/get-ticket-type db-spec ticket-type-id)]
        (is (= (dissoc ticket-type :created-at :updated-at) expected))))))

