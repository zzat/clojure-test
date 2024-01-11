(ns swift-ticketing.model.booking
  (:require [swift-ticketing.db.booking :as booking]
            [swift-ticketing.worker :as worker]))

(defn get-booking-status [db-spec booking-id]
  (booking/get-booking-status db-spec booking-id))

(defn make-payment [message-queue booking-id]
  (worker/request-ticket-booking message-queue {:booking-id booking-id}))

(defn cancel-booking [message-queue booking-id]
  (worker/request-ticket-cancellation message-queue {:booking-id booking-id}))
