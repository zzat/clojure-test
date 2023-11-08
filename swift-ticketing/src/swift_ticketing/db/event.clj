(ns swift-ticketing.db.event
  (:require [honey.sql :as sql]))

(defn insert-event [uid event_id event-req]
  (sql/format {:insert-into :swift_ticketing.event
               :columns [:event_id :event_name :event_description :event_date :organizer_id :venue]
               :values [[event_id
                         (:name event-req)
                         (:description event-req)
                         [:cast (:date event-req) :date]
                         [:cast uid :uuid]
                         (:venue event-req)]]}))
