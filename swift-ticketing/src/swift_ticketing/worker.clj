(ns swift-ticketing.worker
  (:require [clojure.core.async :as async]
            [swift-ticketing.db.event :as event]
            [swift-ticketing.db.ticket :as ticket]
            [swift-ticketing.db.booking :as booking]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as date-time]
            [clojure.core :as c])
  (:import [java.time Instant Duration]))

(def ticket-queue (async/chan))

(def reserve-event "RESERVE")
(def book-event "BOOK")

(defn add-ticket-request-to-queue [request]
  (async/go (async/>! ticket-queue request)))

(defn add-reserve-ticket-request-to-queue [request]
  (let [event-type reserve-event
        booking-id (:booking-id request)
        ticket-ids (:ticket-ids request)
        ticket-type-id (:ticket-type-id request)
        quantity (:quantity request)]
    (add-ticket-request-to-queue {:event event-type
                                  :data {:booking-id booking-id
                                         :ticket-ids ticket-ids
                                         :ticket-type-id ticket-type-id
                                         :quantity quantity}})))

(defn add-book-ticket-request-to-queue [request]
  (let [event-type book-event
        booking-id (:booking-id request)]
    (add-ticket-request-to-queue {:event event-type
                                  :data {:booking-id booking-id}})))

(defn handle-reserve-event [db-spec request]
  (let [booking-id (get-in request [:data :booking-id])
        ticket-ids (get-in request [:data :ticket-ids])
        ticket-type-id (get-in request [:data :ticket-type-id])
        ticket-quantity (get-in request [:data :quantity])]
    (jdbc/with-transaction [tx db-spec]
      (let [selected-rows (jdbc/execute! tx (ticket/lock-unbooked-tickets ticket-ids ticket-type-id ticket-quantity))
            selected-ticket-ids (map #(:ticket/ticket_id %) selected-rows)
            quantity-available? (= (count selected-rows) ticket-quantity)
            booking-status (if quantity-available? booking/PAYMENTPENDING booking/REJECTED)
            reservation-timelimit-seconds (:ticket_type/reservation_timelimit_seconds (first selected-rows))
            current-time (Instant/now)
            reservation-expiration-time (if (nil? reservation-timelimit-seconds)
                                          nil
                                          (.plus current-time
                                                 (Duration/ofSeconds reservation-timelimit-seconds)))]
              ; (println (ticket/update-ticket-booking-id selected-ticket-ids booking-id))
        (when quantity-available?
          (jdbc/execute! tx (ticket/reserve-tickets selected-ticket-ids booking-id reservation-expiration-time)))
        (jdbc/execute! tx (booking/update-booking-status booking-id booking-status))))))

(defn handle-book-event [db-spec request]
  (jdbc/with-transaction [tx db-spec]
    (let [booking-id (get-in request [:data :booking-id])
          selected-ticket-ids (->> (jdbc/execute! tx (ticket/lock-reserved-tickets booking-id))
                                   (map #(:ticket/ticket_id %)))]
      (jdbc/execute! tx (ticket/confirm-tickets selected-ticket-ids))
      (jdbc/execute! tx (booking/update-booking-status booking-id booking/CONFIRMED)))))

(defn process-ticket-requests [thread-id db-spec]
  (async/go
    (while true
      (try
        (let [request (async/<! ticket-queue)
              event-type (:event request)]
          (println "Got Message:")
          (println request)
          (cond
            (= event-type reserve-event) (handle-reserve-event db-spec request)
            (= event-type book-event) (handle-book-event db-spec request)
            :else (println "Worker: Unknown event")))
        (catch Exception e
          (println (str "Exception in thread #" thread-id " :" e)))))))

(defn workers [db-spec total-workers]
  (dotimes [i total-workers]
    (.start (Thread. #(process-ticket-requests i db-spec)))))
