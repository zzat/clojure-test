(ns swift-ticketing.handlers
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.spec.alpha :as s]
            [swift-ticketing.db.event :as event]
            [swift-ticketing.specs :as specs]
            [swift-ticketing.db.ticket :as ticket]
            [swift-ticketing.db.booking :as booking]
            [swift-ticketing.worker :as worker]))

(defn respond-json [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body body})

(defn respond-400 [body]
  (respond-json 400 body))

(defn respond-201 [body]
  (respond-json 201 body))

(defn respond-200 [body]
  (respond-json 200 body))

(defn validate-req [req spec handler]
  (if (s/valid? spec req)
    (handler)
    (respond-400 (s/explain-data spec req))))

(defn- get-events [db-spec venue from to]
  (event/get-events db-spec venue from to))

(defn get-events-handler [db-spec venue from to]
  (let [params {:venue venue :from from :to to}
        on-success (fn []
                     (respond-200
                      (get-events db-spec venue from to)))]
    (validate-req params ::specs/get-event-params on-success)))

(defn- get-event [db-spec event-id]
  (event/get-event-with-tickets db-spec event-id))

(defn get-event-handler [db-spec event-id]
  (letfn [(on-success []
            (respond-200 (get-event db-spec event-id)))]
    (validate-req event-id ::specs/event-id on-success)))

(defn- create-event [db-spec uid event-req]
  (let [event-id (java.util.UUID/randomUUID)]
    (event/insert-event db-spec uid event-id event-req)
    event-id))

(defn create-event-handler [db-spec uid event-req]
  (letfn [(on-success []
            (respond-201
             {"event_id" (create-event db-spec uid event-req)}))]
    (validate-req event-req ::specs/create-event-params on-success)))

(defn- create-tickets [db-spec uid event-id ticket-req]
  (let [seat-type (:seat_type ticket-req)
        tickets-map (if (= seat-type ticket/NAMED)
                      (:seats ticket-req)
                      (map (fn [_] {:name ""}) (range (:quantity ticket-req))))
        price (:price ticket-req)
        ticket-type-id (java.util.UUID/randomUUID)
        tickets
        (map (fn [m] (assoc m :ticket-id (java.util.UUID/randomUUID))) tickets-map)]
    (ticket/insert-ticket-type db-spec event-id ticket-type-id ticket-req)
    (ticket/insert-tickets db-spec ticket-type-id tickets price)

    {:ticket-type-id ticket-type-id
     :tickets tickets}))

(defn create-tickets-handler [db-spec uid event-id ticket-req]
  (letfn [(on-success []
            (let [{:keys [ticket-type-id tickets]}
                  (create-tickets db-spec uid event-id ticket-req)]
              (respond-201
               {"ticket_type_id" ticket-type-id
                "tickets" tickets})))]
    (and
     (s/valid? ::specs/event-id event-id)
     (validate-req ticket-req ::specs/create-tickets-params on-success))))

(defn- reserve-ticket [db-spec uid event-id booking-req]
  (let [booking-id (java.util.UUID/randomUUID)
        req-ticket-ids (:ticket_ids booking-req)
        ticket-ids (when-not (nil? req-ticket-ids)
                     (map #(java.util.UUID/fromString %) req-ticket-ids))]
    (booking/insert-booking db-spec uid booking-id)
    (worker/add-reserve-ticket-request-to-queue {:booking-id booking-id
                                                 :ticket-type-id (:ticket_type_id booking-req)
                                                 :ticket-ids ticket-ids
                                                 :quantity (:quantity booking-req)})
    booking-id))

(defn reserve-ticket-handler [db-spec uid event-id booking-req]
  (letfn [(on-success []
            (respond-201
             {"booking_id"
              (reserve-ticket db-spec uid event-id booking-req)}))]
    (and
     (s/valid? ::specs/event-id event-id)
     (validate-req booking-req ::specs/reserve-tickets-params on-success))))

(defn- post-payment [db-spec booking-id]
  (worker/add-book-ticket-request-to-queue {:booking-id booking-id}))

(defn post-payment-handler [db-spec booking-id]
  (letfn [(on-success []
            (do
              (post-payment db-spec booking-id)
              (respond-200
               {"booking_id" booking-id})))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn- cancel-booking [db-spec booking-id]
  (worker/add-cancel-ticket-request-to-queue {:booking-id booking-id})
  )

(defn cancel-booking-handler [db-spec booking-id]
  (letfn [(on-success []
            (do
              (cancel-booking db-spec booking-id)
              (respond-200 {"booking_id" booking-id})))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn- get-booking-status [db-spec booking-id]
  (booking/get-booking-status db-spec booking-id))

(defn get-booking-status-handler [db-spec booking-id]
  (letfn [(on-success []
            (respond-200
              {"booking_status"
                (get-booking-status db-spec booking-id)}))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn- get-tickets [db-spec ticket-type-id]
  (ticket/get-unbooked-tickets db-spec ticket-type-id))

(defn get-tickets-handler [db-spec ticket-type-id]
  (letfn [(on-success []
            (respond-200 (get-tickets db-spec ticket-type-id)))]
  (validate-req ticket-type-id ::specs/ticket-type-id on-success)))

(defn- get-tickets-by-booking-id [db-spec booking-id]
  (ticket/get-tickets-by-booking-id db-spec booking-id))

(defn get-tickets-by-booking-id-handler [db-spec booking-id]
  (letfn [(on-success []
            (respond-200 (get-tickets-by-booking-id db-spec booking-id)))]
    (validate-req booking-id ::specs/booking-id on-success)))
