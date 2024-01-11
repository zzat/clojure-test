(ns swift-ticketing.model.ticket-test
  (:require [clojure.test :refer [deftest testing is]]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.model.ticket :as ticket]
            [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.worker :as worker]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(deftest create-tickets-test
  (testing "Creating Tickets"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          event-id (str (random-uuid))
          ticket-req (cske/transform-keys csk/->kebab-case-keyword
                                          (factory/seated-ticket-request))
          insert-ticket-type-args (atom {})
          insert-tickets-args (atom {})]
      (with-redefs
       [db-ticket/insert-ticket-type
        (fn [dbs eid _ req]
          (reset! insert-ticket-type-args {:db-spec dbs
                                           :event-id eid
                                           :ticket-request req}))
        db-ticket/insert-tickets
        (fn [dbs _ _ price]
          (reset! insert-tickets-args {:db-spec dbs
                                       :price price}))]
        (let [{:keys [ticket-type-id tickets]}
              (ticket/create-tickets db-spec test-user-id event-id ticket-req)]
          (is (uuid? ticket-type-id))
          (is (not (nil? tickets)))
          (is (= {:db-spec db-spec
                  :event-id event-id
                  :ticket-request ticket-req}
                 @insert-ticket-type-args))
          (is (= {:db-spec db-spec
                  :price (:price ticket-req)}
                 @insert-tickets-args)))))))

(deftest reserve-ticket-test
  (testing "Reserving tickets"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          message-queue :message-queue
          event-id (str (random-uuid))
          ticket-req (cske/transform-keys
                      csk/->kebab-case-keyword
                      (factory/mk-reserve-seated-ticket-request
                       [(random-uuid)]))
          insert-booking-args (atom {})
          add-reserve-ticket-args (atom {})]
      (with-redefs
       [db-booking/insert-booking
        (fn [dbs uid bid status]
          (reset! insert-booking-args {:db-spec dbs
                                       :user-id uid
                                       :booking-status status}))
        worker/add-reserve-ticket-request-to-queue
        (fn [mq req]
          (reset! add-reserve-ticket-args
                  {:message-queue mq
                   :ticket-type-id (:ticket-type-id req)
                   :quantity (:quantity req)}))]
        (is (uuid?
             (ticket/reserve-ticket
              db-spec message-queue test-user-id event-id ticket-req)))
        (is (= {:db-spec db-spec
                :user-id test-user-id
                :booking-status db-booking/INPROCESS}
               @insert-booking-args))
        (is (= {:message-queue message-queue
                :ticket-type-id (:ticket-type-id ticket-req)
                :quantity (:quantity ticket-req)}
               @add-reserve-ticket-args))))))

(deftest get-tickets-test
  (testing "Fetching unbooked tickets"
    (let [{:keys [db-spec]} fixtures/test-env
          ticket-type-id (str (random-uuid))
          get-unbooked-tickets-args (atom {})
          results (repeatedly (inc (rand-int 10)) factory/mk-ticket)]
      (with-redefs
       [db-ticket/get-unbooked-tickets
        (fn [dbs tid]
          (reset! get-unbooked-tickets-args {:db-spec dbs
                                             :ticket-type-id tid})
          results)]
        (is (= results (ticket/get-tickets db-spec ticket-type-id)))
        (is (= {:db-spec db-spec
                :ticket-type-id ticket-type-id}
               @get-unbooked-tickets-args))))))

(deftest get-tickets-by-booking-id-test
  (testing "Fetching tickets by booking id"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (str (random-uuid))
          get-tickets-by-booking-id-args (atom {})
          results (repeatedly (inc (rand-int 10)) factory/mk-ticket)]
      (with-redefs
       [db-ticket/get-tickets-by-booking-id
        (fn [dbs bid]
          (reset! get-tickets-by-booking-id-args {:db-spec dbs
                                                  :booking-id bid})
          results)]
        (is (= results (ticket/get-tickets-by-booking-id db-spec booking-id)))
        (is (= {:db-spec db-spec
                :booking-id booking-id}
               @get-tickets-by-booking-id-args))))))
