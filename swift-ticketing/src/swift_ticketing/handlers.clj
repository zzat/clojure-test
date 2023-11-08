(ns swift-ticketing.handlers
  (:require [next.jdbc :as jdbc]
            ; [ring.util.response :as response]
            [swift-ticketing.db.event :as event]
            [swift-ticketing.db.ticket :as ticket]))

(defn create-event [db-spec uid event-req]
  (let [event-id (java.util.UUID/randomUUID)]
    (jdbc/execute! db-spec (event/insert-event uid event-id event-req))
    {:status 201
     :body {"event_id" event-id}}))

(defn create-tickets [db-spec uid event-id ticket-req]
  (let [ticket-ids
        (map (fn [_] (java.util.UUID/randomUUID)) (range (:quantity ticket-req)))]
    (jdbc/execute! db-spec (ticket/insert-tickets uid event-id ticket-ids ticket-req))
    {:status 201
     :body {"tickets" ticket-ids}}))
