(ns swift-ticketing.handlers
  (:require
   [clojure.spec.alpha :as s]
   [clojure.set :as set]
   [swift-ticketing.model.event :as event]
   [swift-ticketing.specs :as specs]
   [clojure.walk :refer [keywordize-keys]]
   [swift-ticketing.model.ticket :as ticket]
   [swift-ticketing.model.booking :as booking]))

(defn respond-json [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body body})

(defn respond-400 [body]
  (respond-json 400 body))

(defn respond-201 [body]
  (respond-json 201 body))

(defn respond-200 [body]
  (respond-json 200 body))

(defn validate-req [req spec handler]
  (if (s/valid? spec req)
    (handler)
    (respond-400 (s/explain-data spec req))))

(defn- get-uid [cookies]
  (get-in cookies ["uid" :value]))

(defn get-events-handler [db-spec request]
  (let [filters (-> request
                    :query-params
                    keywordize-keys)
        on-success (fn []
                     (respond-200
                      (event/get-events db-spec filters)))]
    (validate-req filters ::specs/get-event-params on-success)))

(defn get-event-handler [db-spec request]
  (let [event-id (get-in request [:route-params :event-id])
        on-success (fn []
                     (respond-200 (event/get-event db-spec event-id)))]
    (validate-req event-id ::specs/event-id on-success)))

(defn create-event-handler [db-spec request]
  (let [{:keys [cookies body]} request
        uid (get-uid cookies)
        on-success (fn []
                     (respond-201
                      {"event_id" (event/create-event db-spec uid body)}))]
    (validate-req body ::specs/create-event-params on-success)))

(defn create-tickets-handler [db-spec request]
  (let [{:keys [cookies body route-params]} request
        uid (get-uid cookies)
        event-id (:event-id route-params)
        on-success (fn []
                     (let [{:keys [ticket-type-id tickets]}
                           (ticket/create-tickets db-spec uid event-id body)]
                       (respond-201
                        {"ticket_type_id" ticket-type-id
                         "tickets" (map #(set/rename-keys % {:ticket-id "ticket_id"}) tickets)})))]
    (and
     (s/valid? ::specs/event-id event-id)
     (validate-req body ::specs/create-tickets-params on-success))))

(defn reserve-ticket-handler [db-spec message-queue request]
  (let [{:keys [cookies body route-params]} request
        uid (get-uid cookies)
        event-id (:event-id route-params)
        on-success (fn []
                     (respond-201
                      {"booking_id"
                       (ticket/reserve-ticket db-spec message-queue uid event-id body)}))]
    (and
     (s/valid? ::specs/event-id event-id)
     (validate-req body ::specs/reserve-tickets-params on-success))))

(defn post-payment-handler [db-spec message-queue request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (booking/make-payment db-spec message-queue booking-id)
                     (respond-200
                      {"booking_id" booking-id}))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn cancel-booking-handler [db-spec message-queue request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (booking/cancel-booking db-spec message-queue booking-id)
                     (respond-200 {"booking_id" booking-id}))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn get-booking-status-handler [db-spec request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (respond-200
                      {"booking_status"
                       (booking/get-booking-status db-spec booking-id)}))]
    (validate-req booking-id ::specs/booking-id on-success)))

(defn get-tickets-handler [db-spec request]
  (let [ticket-type-id (get-in request [:query-params "ticket_type_id"])
        on-success (fn []
                     (respond-200 (ticket/get-tickets db-spec ticket-type-id)))]
    (validate-req ticket-type-id ::specs/ticket-type-id on-success)))

(defn get-tickets-by-booking-id-handler [db-spec request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (respond-200 (ticket/get-tickets-by-booking-id db-spec booking-id)))]
    (validate-req booking-id ::specs/booking-id on-success)))
