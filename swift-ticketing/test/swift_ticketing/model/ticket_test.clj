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
          insert-ticket-type-called-with-correct-args (atom false)
          insert-tickets-called-with-correct-args (atom false)]
      (with-redefs
       [db-ticket/insert-ticket-type
        (fn [dbs eid tid req]
          (when (and (= db-spec dbs)
                     (= event-id eid)
                     (uuid? tid)
                     (= ticket-req req))
            (reset! insert-ticket-type-called-with-correct-args true)))
        db-ticket/insert-tickets
        (fn [dbs tid _ price]
          (when (and (= db-spec dbs)
                     (uuid? tid)
                     (= (:price ticket-req) price))
            (reset! insert-tickets-called-with-correct-args true)))]
        (let [{:keys [ticket-type-id tickets]}
              (ticket/create-tickets db-spec test-user-id event-id ticket-req)]
          (is (uuid? ticket-type-id))
          (is (not (nil? tickets)))
          (is @insert-ticket-type-called-with-correct-args)
          (is @insert-tickets-called-with-correct-args))))))

(deftest reserve-ticket-test
  (testing "Reserving tickets"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          message-queue :message-queue
          event-id (str (random-uuid))
          ticket-req (cske/transform-keys csk/->kebab-case-keyword
                                          (factory/mk-reserve-seated-ticket-request
                                           [(random-uuid)]))
          insert-booking-called-with-correct-args (atom false)
          add-reserve-ticket-called-with-correct-args (atom false)]
      (with-redefs
       [db-booking/insert-booking
        (fn [dbs uid bid status]
          (when (and (= db-spec dbs)
                     (= test-user-id uid)
                     (uuid? bid)
                     (= db-booking/INPROCESS status))
            (reset! insert-booking-called-with-correct-args true)))
        worker/add-reserve-ticket-request-to-queue
        (fn [mq req]
          (when (and (= message-queue mq)
                     (uuid? (:booking-id req))
                     (= (:ticket_type_id ticket-req) (:ticket-type-id req))
                     (= (:quantity ticket-req) (:quantity req)))
            (reset! add-reserve-ticket-called-with-correct-args true)))]
        (is (uuid?
             (ticket/reserve-ticket
              db-spec message-queue test-user-id event-id ticket-req)))
        (is @insert-booking-called-with-correct-args)
        (is @add-reserve-ticket-called-with-correct-args)))))

(deftest get-tickets-test
  (testing "Fetching unbooked tickets"
    (let [{:keys [db-spec]} fixtures/test-env
          ticket-type-id (str (random-uuid))]
      (with-redefs [db-ticket/get-unbooked-tickets
                    (fn [dbs tid]
                      (when (and (= db-spec dbs)
                                 (= ticket-type-id tid))
                        :ok))]
        (is (= :ok (ticket/get-tickets db-spec ticket-type-id)))))))

(deftest get-tickets-by-booking-id-test
  (testing "Fetching tickets by booking id"
    (let [{:keys [db-spec]} fixtures/test-env
          booking-id (str (random-uuid))]
      (with-redefs [db-ticket/get-tickets-by-booking-id
                    (fn [dbs bid]
                      (when (and (= db-spec dbs)
                                 (= booking-id bid))
                        :ok))]
        (is (= :ok (ticket/get-tickets-by-booking-id db-spec booking-id)))))))
