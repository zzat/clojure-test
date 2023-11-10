(ns swift-ticketing.worker
  (:require [clojure.core.async :as async]
            [swift-ticketing.db.event :as event]
            [swift-ticketing.db.ticket :as ticket]
            [swift-ticketing.db.booking :as booking]
            [next.jdbc :as jdbc]))

(def ticket-queue (async/chan))

(defn add-ticket-request-to-queue [event-id request]
  (let [event "BookTicket"
        booking-id (:booking-id request)
        ticket-type (:ticket-type request)
        quantity (:quantity request)]
    (async/go (async/>! ticket-queue {:event event
                                      :data {:booking-id booking-id
                                             :ticket-type ticket-type
                                             :quantity quantity
                                             :event-id event-id}}))))

(defn process-ticket-requests [thread-id db-spec]
  (async/go
    (while true
      (try
        (let [request (async/<! ticket-queue)
              booking-id (get-in request [:data :booking-id])
              event-id (get-in request [:data :event-id])
              ticket-type (get-in request [:data :ticket-type])
              ticket-quantity (get-in request [:data :quantity])]
          (println "Got Message:")
          (println request)
          (jdbc/with-transaction [tx db-spec]
            (let [selected-rows (jdbc/execute! db-spec (ticket/lock-unbooked-tickets ticket-type event-id ticket-quantity))
                  selected-ticket-ids (map #(:ticket/ticket_id %) selected-rows)]
              (println (ticket/update-ticket-booking-id selected-ticket-ids booking-id))
              (jdbc/execute! db-spec (ticket/update-ticket-booking-id selected-ticket-ids booking-id)))
            (jdbc/execute! db-spec (booking/update-booking-status booking-id))))
        (catch Exception e
          (println (str "Exception in thread #" thread-id " :" e)))))))

(defn workers [db-spec total-workers]
  (dotimes [i total-workers]
    (.start (Thread. #(process-ticket-requests i db-spec)))))
