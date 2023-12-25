(ns swift-ticketing.model.ticket
  (:require [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.booking :as db-booking]
            [swift-ticketing.worker :as worker]))

(defn create-tickets [db-spec uid event-id ticket-req]
  (let [seat-type (:seat_type ticket-req)
        tickets-map (if (= seat-type db-ticket/NAMED)
                      (:seats ticket-req)
                      (map (fn [_] {:name ""}) (range (:quantity ticket-req))))
        price (:price ticket-req)
        ticket-type-id (java.util.UUID/randomUUID)
        tickets
        (map (fn [m] (assoc m :ticket-id (java.util.UUID/randomUUID))) tickets-map)]
    (db-ticket/insert-ticket-type db-spec event-id ticket-type-id ticket-req)
    (db-ticket/insert-tickets db-spec ticket-type-id tickets price)
    {:ticket-type-id ticket-type-id
     :tickets tickets}))

(defn reserve-ticket [db-spec message-queue uid event-id booking-req]
  (let [booking-id (java.util.UUID/randomUUID)
        req-ticket-ids (:ticket_ids booking-req)
        ticket-ids (when-not (nil? req-ticket-ids)
                     (map #(java.util.UUID/fromString %) req-ticket-ids))]
    (db-booking/insert-booking db-spec uid booking-id db-booking/INPROCESS)
    (worker/add-reserve-ticket-request-to-queue
     message-queue
     {:booking-id booking-id
      :ticket-type-id (:ticket_type_id booking-req)
      :ticket-ids ticket-ids
      :quantity (:quantity booking-req)})
    booking-id))

(defn get-tickets [db-spec ticket-type-id]
  (db-ticket/get-unbooked-tickets db-spec ticket-type-id))

(defn get-tickets-by-booking-id [db-spec booking-id]
  (db-ticket/get-tickets-by-booking-id db-spec booking-id))
