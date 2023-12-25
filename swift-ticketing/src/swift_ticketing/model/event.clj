(ns swift-ticketing.model.event
  (:require [swift-ticketing.db.event :as db-event]))

(defn get-events [db-spec filters]
  (db-event/get-events db-spec filters))

(defn get-event [db-spec event-id]
  (db-event/get-event-with-tickets db-spec event-id))

(defn create-event [db-spec uid event-req]
  (let [event-id (java.util.UUID/randomUUID)]
    (db-event/insert-event db-spec uid event-id event-req)
    event-id))
