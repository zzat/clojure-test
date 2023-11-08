(ns swift-ticketing.handlers
  (:require [next.jdbc :as jdbc]
            ; [ring.util.response :as response]
            [swift-ticketing.db.event :as query]))

(defn create-event [db-spec uid event-req]
  (let [event_id (java.util.UUID/randomUUID)]
  (jdbc/execute! db-spec (query/insert-event uid event_id event-req))
  {:status 201
   :body {"event_id" event_id}}))
