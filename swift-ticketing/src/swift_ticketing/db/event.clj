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
