(ns swift-ticketing.db.ticket
  (:require [honey.sql :as sql])
  (:import [java.time Instant]))

(defonce RESERVED "Reserved")
(defonce AVAILABLE "Available")
(defonce BOOKED "BOOKED")

(defn insert-tickets [uid event_id ticket-ids ticket-req]
  (let [make-ticket (fn [ticket-id] [[:cast ticket-id :uuid]
                                     (:name ticket-req)
                                     (:description ticket-req)
                                     [:cast event_id :uuid]
                                     (:price ticket-req)])
        tickets (map make-ticket ticket-ids)]
    (sql/format {:insert-into :ticket
                 :columns [:ticket_id :ticket_name :ticket_description :event_id :ticket_price]
                 :values tickets})))

(defn lock-unbooked-tickets [ticket-ids ticket-type ticket-quantity]
  (let [current-time (Instant/now)
        reservation-expired [:and
                             [:= :ticket.ticket_status RESERVED]
                             [:> current-time :ticket.reservation_expiration_time]]
        tickets-available [:= :ticket.ticket_status AVAILABLE]
        where-clause (if (nil? ticket-ids)
                       [:and
                        [:= :ticket.ticket_type_id [:cast ticket-type :uuid]]
                        [:or tickets-available reservation-expired]]
                       [:in :ticket_id ticket-ids])
        quantity (if (nil? ticket-ids) ticket-quantity (count ticket-ids))]

    (sql/format {:select [:*]
                 :from :ticket
                 :inner-join [[:ticket_type :tt] [:= :ticket.ticket_type_id :tt.ticket_type_id]]
                 :where where-clause
                 :for [:update :skip-locked]
                 :limit quantity})))

(defn lock-reserved-tickets [booking-id]
  (sql/format {:select [:*]
               :from :ticket
               :where [:and [:= :ticket_status RESERVED] [:= :booking_id [:cast booking-id :uuid]]]
               :for [:update :skip-locked]}))

(defn reserve-tickets [ticket-ids booking-id reservation-expiration-time]
  (sql/format {:update :ticket
               :set {:booking_id [:cast booking-id :uuid]
                     :ticket_status RESERVED
                     :reservation_expiration_time reservation-expiration-time}
               :where [:in :ticket_id ticket-ids]}))

(defn confirm-tickets [ticket-ids]
  (sql/format {:update :ticket
               :set {:ticket_status BOOKED
                     :reservation_expiration_time nil}
               :where [:in :ticket_id ticket-ids]}))

(defn update-ticket-booking-id [ticket-ids booking-id]
  (sql/format {:update :ticket
               :set {:booking_id [:cast booking-id :uuid]}
               :where [:in :ticket_id ticket-ids]}))

(defn get-tickets-by-booking-id [booking-id]
  (sql/format {:select [:t.*]
               :from [[:booking :b]]
               :inner-join [[:ticket :t] [:= :b.booking_id :t.booking_id]]
               :where [:= :b.booking_id [:cast booking-id :uuid]]}))
