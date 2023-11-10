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

(defn lock-unbooked-tickets [ticket-type event-id ticket-quantity]
  (sql/format {:select [:*] :from :swift_ticketing.ticket
               :where [:and
                       [:= :event_id [:cast event-id :uuid]]
                       [:= :ticket_name ticket-type]
                       [:= :booking_id nil]]
               :for [:update :skip-locked] 
               :limit ticket-quantity}))

(defn update-ticket-booking-id [ticket-ids booking-id]
  (sql/format {:update :swift_ticketing.ticket
               :set {:booking_id [:cast booking-id :uuid]}
               :where [:in :ticket_id ticket-ids]}))
