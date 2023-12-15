(ns swift-ticketing.db.ticket
  (:require [honey.sql :as sql]
            [swift-ticketing.db.query :refer [run-query!]])
  (:import [java.time Instant]))

;; ticket_status
(defonce RESERVED "Reserved")
(defonce AVAILABLE "Available")
(defonce BOOKED "Booked")

;; seat_type
(defonce NAMED "Named")
(defonce GENERAL "General")

(defn insert-ticket-type [db-spec event-id ticket-type-id ticket-req]
  (run-query!
   db-spec
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
                            [:cast seat-type :seat_type]]]}))))

(defn insert-tickets [db-spec ticket-type-id tickets price]
  (run-query!
   db-spec
   (let [make-ticket (fn [ticket] [[:cast (:ticket-id ticket) :uuid]
                                   (:name ticket)
                                   price
                                   [:cast AVAILABLE :ticket_status]
                                   [:cast ticket-type-id :uuid]])
         ticket-rows (map make-ticket tickets)]
     (sql/format {:insert-into :ticket
                  :columns [:ticket_id :ticket_name :ticket_price :ticket_status :ticket_type_id]
                  :values ticket-rows}))))

(defn select-unbooked-tickets [db-spec should-lock? ticket-ids ticket-type-id ticket-quantity]
  (run-query!
   db-spec
   (let [current-time [:cast (.toString (Instant/now)) :timestamptz]
         reservation-expired [:and
                              [:= :ticket.ticket_status [:cast RESERVED :ticket_status]]
                              [:or
                               [:> current-time :ticket.reservation_expiration_time]
                               [:= :ticket.reservation-expiration-time nil]]]
         tickets-available [:= :ticket.ticket_status [:cast AVAILABLE :ticket_status]]
         where-clause (if (or (nil? ticket-ids) (empty? ticket-ids))
                        [:and
                         [:= :ticket.ticket_type_id [:cast ticket-type-id :uuid]]
                         [:or tickets-available reservation-expired]]
                        [:and
                         [:in :ticket_id ticket-ids]
                         [:or tickets-available reservation-expired]])
         quantity (if (nil? ticket-ids) ticket-quantity (count ticket-ids))
         base-query {:select [:*]
                     :from :ticket
                     :inner-join [[:ticket_type :tt] [:= :ticket.ticket_type_id :tt.ticket_type_id]]
                     :where where-clause
                     :limit quantity}
         query (if should-lock?
                 (conj base-query [:for [:update :skip-locked]])
                 base-query)]
     (sql/format query))))

(defn lock-unbooked-tickets [db-spec ticket-ids ticket-type-id ticket-quantity]
  (select-unbooked-tickets db-spec true ticket-ids ticket-type-id ticket-quantity))

(defn select-reserved-tickets [db-spec should-lock? booking-id]
  (run-query!
   db-spec
   (let [base-query {:select [:*]
                     :from :ticket
                     :where [:and
                             [:= :ticket_status [:cast RESERVED :ticket_status]]
                             [:= :booking_id [:cast booking-id :uuid]]]}
         query (if should-lock?
                 (conj base-query [:for [:update :skip-locked]])
                 base-query)]
     (sql/format query))))

(defn lock-reserved-tickets [db-spec booking-id]
  (select-reserved-tickets db-spec true booking-id))

(defn reserve-tickets [db-spec ticket-ids booking-id reservation-expiration-time]
  (run-query!
   db-spec
   (sql/format {:update :ticket
                :set {:booking_id [:cast booking-id :uuid]
                      :ticket_status [:cast RESERVED :ticket_status]
                      :reservation_expiration_time reservation-expiration-time}
                :where [:in :ticket_id ticket-ids]})))

(defn reset-ticket-status [db-spec ticket-ids status]
  (run-query!
   db-spec
   (sql/format {:update :ticket
                :set {:ticket_status [:cast status :ticket_status]
                      :reservation_expiration_time nil}
                :where [:in :ticket_id ticket-ids]})))

(defn confirm-tickets [db-spec ticket-ids]
  (reset-ticket-status db-spec ticket-ids BOOKED))

(defn cancel-tickets [db-spec ticket-ids]
  (reset-ticket-status db-spec ticket-ids AVAILABLE))

(defn get-tickets-by-booking-id [db-spec booking-id]
  (run-query!
   db-spec
   (sql/format {:select [:t.*]
                :from [[:booking :b]]
                :inner-join [[:ticket :t] [:= :b.booking_id :t.booking_id]]
                :where [:= :b.booking_id [:cast booking-id :uuid]]})))

(defn get-unbooked-tickets [db-spec ticket-type-id]
  (run-query!
   db-spec
   (let [current-time [:cast (.toString (Instant/now)) :timestamptz]
         reservation-expired [:and
                              [:= :ticket.ticket_status [:cast RESERVED :ticket_status]]
                              [:or
                               [:> current-time :ticket.reservation_expiration_time]
                               [:= :ticket.reservation-expiration-time nil]]]
         tickets-available [:= :ticket.ticket_status [:cast AVAILABLE :ticket_status]]]
     (sql/format {:select [:*]
                  :from :ticket
                  :where [:and
                          [:or tickets-available reservation-expired]
                          [:= :ticket_type_id [:cast ticket-type-id :uuid]]]}))))
