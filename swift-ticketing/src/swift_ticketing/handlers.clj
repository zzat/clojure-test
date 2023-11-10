(ns swift-ticketing.handlers
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            ; [ring.util.response :as response]
            [swift-ticketing.db.event :as event]
            [swift-ticketing.db.ticket :as ticket]
            [swift-ticketing.db.booking :as booking]
            [swift-ticketing.worker :as worker]))

(defn get-event [db-spec venue from to]
  (let [events (jdbc/execute! db-spec (event/get-event venue from to) {:builder-fn rs/as-unqualified-maps})]
    {:status 200
     :body events}))

(defn create-event [db-spec uid event-req]
  (let [event-id (java.util.UUID/randomUUID)]
    (jdbc/execute! db-spec (event/insert-event uid event-id event-req))
    {:status 201
     :body {"event_id" event-id}}))

(defn create-tickets [db-spec uid event-id ticket-req]
  (let [ticket-ids
        (map (fn [_] (java.util.UUID/randomUUID)) (range (:quantity ticket-req)))]
    (jdbc/execute! db-spec (ticket/insert-tickets uid event-id ticket-ids ticket-req))
    {:status 201
     :body {"tickets" ticket-ids}}))

(defn book-ticket [db-spec uid event-id booking-req]
  (let [booking-id (java.util.UUID/randomUUID)
        quantity (:quantity booking-req)]
    (jdbc/execute! db-spec (booking/insert-booking uid booking-id))
    (worker/add-ticket-request-to-queue event-id {:booking-id booking-id
                                                  :ticket-type (:ticket_name booking-req)
                                                  :quantity quantity})
    {:status 201
     :body {"booking_id" booking-id}}))

(defn get-booking-status [db-spec uid booking-id]
  (let [result (:booking/booking_status (jdbc/execute-one! db-spec (booking/get-booking-status uid booking-id)))]
    {:status 200
     :body {"booking_status" result}}))
