(ns swift-ticketing.db.event
  (:require [honey.sql :as sql]
            [swift-ticketing.db.ticket :as ticket]
            [next.jdbc.date-time :as date-time])
  (:import [java.time Instant]))

(defn insert-event [uid event_id event-req]
  (sql/format {:insert-into :event
               :columns [:event_id :event_name :event_description :event_date :organizer_id :venue]
               :values [[event_id
                         (:name event-req)
                         (:description event-req)
                         [:cast (:date event-req) :date]
                         [:cast uid :uuid]
                         (:venue event-req)]]}))

(defn get-events [venue from to]
  (sql/format {:select [:event_id :event_name :event_description :event_date :venue] :from :event
               :where [:and
                       (if (nil? venue) [true] [:= :venue venue])
                       (if (nil? from) [true] [:>= :event_date [:cast from :date]])
                       (if (nil? to) [true] [:<= :event_date [:cast to :date]])]}))

(defn get-event [event-id]
  (let [current-time (Instant/now)
        reservation-expired [:and
                             [:= :ticket.ticket_status [:cast ticket/RESERVED :ticket_status]]
                             [:> current-time :ticket.reservation_expiration_time]]
        tickets-available [:= :ticket.ticket_status [:cast ticket/AVAILABLE :ticket_status]]]
    (sql/format {:select [:e.event_id
                          :event_name
                          :event_description
                          :event_date
                          :venue
                          :tt.ticket_type
                          :tt.ticket_type_id
                          [[:count :ticket_id] :ticket_count]
                          [[:min :ticket_name] :ticket_name]
                          [[:min :tt.ticket_type_description] :ticket_description]
                          [[:min :ticket_price] :ticket_price]]
                 :from [[:event :e]]
                 :left-join [[:ticket_type :tt] [:= :e.event_id :tt.event_id]
                             [:ticket] [:= :ticket.ticket_type_id :tt.ticket_type_id]]
                 :where [:and
                         [:or tickets-available reservation-expired]
                         [:= :e.event_id [:cast event-id :uuid]]]
                 :group-by [:e.event_id :tt.ticket_type_id]})))
