(ns swift-ticketing.db.event-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [swift-ticketing.fixtures :as fixtures]
            [swift-ticketing.factory :as factory]
            [swift-ticketing.db.event :as db-event]
            [swift-ticketing.db.ticket :as db-ticket])
  (:import [java.time Instant Duration]))

(use-fixtures :each fixtures/clear-tables)

(defn- format-date [date]
  (.format
   (java.text.SimpleDateFormat. "yyyy-MM-dd") date))

(defn- parse-date [date]
  (.parse
   (java.text.SimpleDateFormat. "yyyy-MM-dd") date))

(deftest insert-event-test
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        event-request (cske/transform-keys csk/->kebab-case-keyword
                                           (factory/event-request))
        event-id (random-uuid)
        expected {:event-id event-id
                  :event-name (:name event-request)
                  :event-description (:description event-request)
                  :event-date (:date event-request)
                  :venue (:venue event-request)}]
    (testing "Insert Event query"
      (db-event/insert-event db-spec test-user-id event-id event-request)
      (let [event (db-event/get-event db-spec event-id)
            actual (-> event
                       (dissoc :organizer-id :created-at :updated-at)
                       (update :event-date format-date))]
        (is (= expected actual))
        (is (uuid? (:organizer-id event)))))))

(deftest get-events-test
  (testing "Get events query"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          event-request (cske/transform-keys csk/->kebab-case-keyword
                                             (factory/event-request))
          event-id (random-uuid)
          expected {:event-id event-id
                    :event-name (:name event-request)
                    :event-description (:description event-request)
                    :event-date (:date event-request)
                    :venue (:venue event-request)}
          prune-event
          (fn [event]
            (-> event
                (dissoc :organizer-id :created-at :updated-at)
                (update :event-date format-date)))]
      (db-event/insert-event db-spec test-user-id event-id event-request)
      (testing "without filters"
        (let [events (->> (db-event/get-events db-spec {})
                          (map prune-event))]
          (is (= [expected] events))))

      (testing "with filters"
        (let [invalid-venue {:venue "invalid"}
              valid-venue {:venue (:venue expected)}
              valid-from {:from (:event-date expected)}
              valid-to {:to (:event-date expected)}
              out-of-range-from {:from (-> (:event-date expected)
                                           parse-date
                                           (.toInstant)
                                           (.plus (Duration/ofDays 1))
                                           (java.util.Date/from)
                                           format-date)}
              out-of-range-to {:to (-> (:event-date expected)
                                       parse-date
                                       (.toInstant)
                                       (.minus (Duration/ofDays 1))
                                       (java.util.Date/from)
                                       format-date)}]

          (println (db-event/get-events db-spec valid-venue))
          (println (map prune-event (db-event/get-events db-spec valid-venue)))
          (is (every?
               #(= [expected] %)
               (->> [valid-venue
                     valid-from
                     valid-to]
                    (map #(map prune-event
                               (db-event/get-events db-spec %))))))
          (is (every?
               #(= [] %)
               (map #(db-event/get-events db-spec %)
                    [invalid-venue
                     out-of-range-from
                     out-of-range-to]))))))))

(deftest get-event-with-tickets-test
  (testing "Get Event with tickets info"
    (let [{:keys [db-spec test-user-id]} fixtures/test-env
          ticket-type-id (random-uuid)
          event-request (cske/transform-keys csk/->kebab-case-keyword
                                             (factory/event-request))
          ticket-request (cske/transform-keys csk/->kebab-case-keyword
                                              (factory/general-ticket-request))
          event-id (random-uuid)
          ticket-name (str (random-uuid))
          ticket-id (random-uuid)
          tickets [{:name ticket-name
                    :ticket-id ticket-id}]
          ticket-price (bigdec (rand-int 1000))
          event-with-tickets {:event-id event-id
                              :event-name (:name event-request)
                              :event-description (:description event-request)
                              :event-date (parse-date (:date event-request))
                              :venue (:venue event-request)
                              :ticket-type (:ticket-type ticket-request)
                              :ticket-type-id ticket-type-id
                              :ticket-description (:description ticket-request)
                              :ticket-name ticket-name
                              :ticket-price ticket-price
                              :ticket-count 1
                              :seat-type (:seat-type ticket-request)}]
      (db-event/insert-event db-spec test-user-id event-id event-request)
      (db-ticket/insert-ticket-type db-spec event-id ticket-type-id ticket-request)
      (db-ticket/insert-tickets db-spec ticket-type-id tickets ticket-price)

      (testing "when tickets are Available"
        (let [result (db-event/get-event-with-tickets db-spec event-id)]
          (is (= [event-with-tickets] result))))

      (testing "when tickets are Booked"
        (db-ticket/reset-ticket-status db-spec [ticket-id] db-ticket/booked)
        (let [result (db-event/get-event-with-tickets db-spec event-id)]
          (is (= [] result))))

      (testing "when tickets are Reserved"
        (let [current-time (Instant/now)
              past-time (.minus current-time
                                (Duration/ofSeconds 1000))
              future-time (.plus current-time
                                 (Duration/ofSeconds 1000))]
          (testing "but reservation time has expired"
            (db-ticket/reset-ticket-status db-spec [ticket-id] db-ticket/reserved past-time)
            (let [result (db-event/get-event-with-tickets db-spec event-id)]
              (is (= [event-with-tickets] result))))
          (testing "but reservation time has not expired"
            (db-ticket/reset-ticket-status db-spec [ticket-id] db-ticket/reserved future-time)
            (let [result (db-event/get-event-with-tickets db-spec event-id)]
              (is (= [] result)))))))))
