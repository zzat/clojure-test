(ns swift-ticketing.db.event
  (:require [honey.sql :as sql]))

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
                       (if (nil? to) [true] [:<= :event_date [:cast to :date]])
                       ]}))

(defn get-event [event-id]
  (sql/format {:select [:e.event_id 
                        :event_name 
                        :event_description 
                        :event_date 
                        :venue 
                        [[:count :ticket_id] :ticket_count] 
                        [[:min :ticket_name] :ticket_name] 
                        [[:min :ticket_description] :ticket_description] 
                        [[:min :ticket_price] :ticket_price]]
               :from [[:event :e]]
               :left-join [[:ticket :t] [:= :e.event_id :t.event_id]]
               :where [:and [:= :t.booking_id nil] [:= :e.event_id [:cast event-id :uuid] ]]
               :group-by [:e.event_id :t.ticket_name]
               }))
