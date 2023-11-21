(ns swift-ticketing.db.ticket
  (:require [honey.sql :as sql]
            [next.jdbc.date-time :as date-time])
  (:import [java.time Instant]
           [java.sql Timestamp]))

;; ticket_status
(defonce RESERVED "Reserved")
(defonce AVAILABLE "Available")
(defonce BOOKED "Booked")

;; seat_type
(defonce NAMED "Named")
(defonce GENERAL "General")

(defn insert-ticket-type [event-id ticket-type-id ticket-req]
  (let [ticket-type (:ticket_type ticket-req)
        description (:description ticket-req)
        reservation-timelimit-seconds (:reservation_limit_in_seconds ticket-req)
        seat-type (:seat_type ticket-req)]
    (sql/format {:insert-into :ticket_type
                 :columns [:ticket_type_id
                           :ticket_type
                           :ticket_type_description
                           :event_id
                           :reservation_timelimit_seconds
                           :seat_type]
                 :values [[ticket-type-id
                           ticket-type
                           description
                           [:cast event-id :uuid]
                           reservation-timelimit-seconds
                           [:cast seat-type :seat_type]]]})))

(defn insert-tickets [ticket-type-id tickets price]
  (let [make-ticket (fn [ticket] [[:cast (:ticket-id ticket) :uuid]
                                  (:name ticket)
                                  price
                                  [:cast AVAILABLE :ticket_status]
                                  [:cast ticket-type-id :uuid]])

        ticket-rows (map make-ticket tickets)]
    (sql/format {:insert-into :ticket
                 :columns [:ticket_id :ticket_name :ticket_price :ticket_status :ticket_type_id]
                 :values ticket-rows})))

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

(defn get-unbooked-tickets [ticket-type-id]
  (let [current-time (Instant/now)
        reservation-expired [:and
                             [:= :ticket.ticket_status [:cast RESERVED :ticket_status]]
                             [:> current-time :ticket.reservation_expiration_time]]
        tickets-available [:= :ticket.ticket_status [:cast AVAILABLE :ticket_status]]]
    (sql/format {:select [:*]
                 :from :ticket
                 :where [:and
                         [:or tickets-available reservation-expired]
                         [:= :ticket_type_id [:cast ticket-type-id :uuid]]]})))
