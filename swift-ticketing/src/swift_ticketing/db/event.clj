(ns swift-ticketing.db.event
  (:require [honey.sql :as sql]
            [swift-ticketing.db.query :refer [run-query!]]
            [swift-ticketing.db.ticket :as ticket]
            [next.jdbc :as jdbc])
  (:import [java.time Instant]))

(defn insert-event [db-spec uid event_id event-req]
  (run-query!
   db-spec
   (sql/format {:insert-into :event
                :columns [:event_id :event_name :event_description :event_date :organizer_id :venue]
                :values [[event_id
                          (:name event-req)
                          (:description event-req)
                          [:cast (:date event-req) :date]
                          [:cast uid :uuid]
                          (:venue event-req)]]})))

(defn get-events [db-spec venue from to]
  (run-query!
   db-spec
   (sql/format {:select [:event_id :event_name :event_description :event_date :venue] :from :event
                :where [:and
                        (if (nil? venue) [true] [:= :venue venue])
                        (if (nil? from) [true] [:>= :event_date [:cast from :date]])
                        (if (nil? to) [true] [:<= :event_date [:cast to :date]])]})))

(defn get-event [db-spec event-id]
  (run-query!
   db-spec
   (sql/format {:select [:event_id :event_name :event_description :event_date :venue] :from :event
                :where [:= :event_id [:cast event-id :uuid]]})))

(defn get-event-with-tickets [db-spec event-id]
  (run-query!
   db-spec
   (let [current-time [:cast (.toString (Instant/now)) :timestamptz]
         reservation-expired [:and
                              [:= :ticket.ticket_status [:cast ticket/RESERVED :ticket_status]]
                              [:or
                               [:> current-time :ticket.reservation_expiration_time]
                               [:= :ticket.reservation-expiration-time nil]]]
         tickets-available [:= :ticket.ticket_status [:cast ticket/AVAILABLE :ticket_status]]]
     (sql/format {:select [:e.event_id
                           :event_name
                           :event_description
                           :event_date
                           :venue
                           :tt.ticket_type
                           :tt.ticket_type_id
                           :tt.seat_type
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
                  :group-by [:e.event_id :tt.ticket_type_id]}))))
