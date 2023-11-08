(ns swift-ticketing.db.ticket
  (:require [honey.sql :as sql]))

(defn insert-tickets [uid event_id ticket-ids ticket-req]
  (let [make-ticket (fn [ticket-id] [[:cast ticket-id :uuid]
                                     (:name ticket-req)
                                     (:description ticket-req)
                                     [:cast event_id :uuid]
                                     (:price ticket-req)])
        tickets (map make-ticket ticket-ids)]
    (sql/format {:insert-into :swift_ticketing.ticket
                 :columns [:ticket_id :ticket_name :ticket_description :event_id :ticket_price]
                 :values tickets})))
