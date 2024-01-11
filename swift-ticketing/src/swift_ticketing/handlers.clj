(ns swift-ticketing.handlers
  (:require
   [malli.core :as m]
   [malli.error :as me]
   [swift-ticketing.model.event :as event]
   [swift-ticketing.specs :as specs]
   [clojure.walk :refer [keywordize-keys]]
   [swift-ticketing.model.ticket :as ticket]
   [swift-ticketing.model.booking :as booking]))

(defn- respond-json [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body body})

(defn- respond-400 [body]
  (respond-json 400 body))

(defn- respond-201 [body]
  (respond-json 201 body))

(defn- respond-200 [body]
  (respond-json 200 body))

(defn- validate-req [req spec handler]
  (if (m/validate spec req)
    (handler)
    (-> spec
        (m/explain req)
        me/humanize
        respond-400)))

(defn- get-uid [cookies]
  (get-in cookies ["uid" :value]))

(defn get-events-handler [db-spec request]
  (let [filters (-> request
                    :query-params
                    keywordize-keys)
        on-success (fn []
                     (respond-200
                      (event/get-events db-spec filters)))]
    (validate-req filters specs/GetEventsParams on-success)))

(defn get-event-handler [db-spec request]
  (let [event-id (get-in request [:route-params :event-id])
        on-success (fn []
                     (respond-200 (event/get-event db-spec event-id)))]
    (validate-req event-id specs/EventId on-success)))

(defn create-event-handler [db-spec request]
  (let [{:keys [cookies body]} request
        uid (get-uid cookies)
        on-success (fn []
                     (respond-201
                      {:event-id (event/create-event db-spec uid body)}))]
    (validate-req body specs/CreateEventParams on-success)))

(defn create-tickets-handler [db-spec request]
  (let [{:keys [cookies body route-params]} request
        uid (get-uid cookies)
        event-id (:event-id route-params)
        on-success (fn []
                     (let [{:keys [ticket-type-id tickets]}
                           (ticket/create-tickets db-spec uid event-id body)]
                       (respond-201
                        {:ticket-type-id ticket-type-id
                         :tickets tickets})))]
    (and
     (m/validate specs/EventId event-id)
     (validate-req body specs/CreateTicketsParams on-success))))

(defn reserve-ticket-handler [db-spec message-queue request]
  (let [{:keys [cookies body route-params]} request
        uid (get-uid cookies)
        event-id (:event-id route-params)
        on-success (fn []
                     (respond-201
                      {:booking-id
                       (ticket/reserve-ticket db-spec message-queue uid event-id body)}))]
    (and
     (m/validate specs/EventId event-id)
     (validate-req body specs/ReserveTicketsParams on-success))))

(defn post-payment-handler [message-queue request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (booking/make-payment message-queue booking-id)
                     (respond-200
                      {:booking-id booking-id}))]
    (validate-req booking-id specs/BookingId on-success)))

(defn cancel-booking-handler [message-queue request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (booking/cancel-booking message-queue booking-id)
                     (respond-200 {:booking-id booking-id}))]
    (validate-req booking-id specs/BookingId on-success)))

(defn get-booking-status-handler [db-spec request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (respond-200
                      {:booking-status
                       (booking/get-booking-status db-spec booking-id)}))]
    (validate-req booking-id specs/BookingId on-success)))

(defn get-tickets-handler [db-spec request]
  (let [ticket-type-id (get-in request [:query-params "ticket_type_id"])
        on-success (fn []
                     (respond-200 (ticket/get-tickets db-spec ticket-type-id)))]
    (validate-req ticket-type-id specs/TicketTypeId on-success)))

(defn get-tickets-by-booking-id-handler [db-spec request]
  (let [booking-id (get-in request [:route-params :booking-id])
        on-success (fn []
                     (respond-200 (ticket/get-tickets-by-booking-id db-spec booking-id)))]
    (validate-req booking-id specs/BookingId on-success)))
