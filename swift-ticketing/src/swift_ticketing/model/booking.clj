(ns swift-ticketing.model.booking
  (:require [swift-ticketing.db.booking :as booking]
            [swift-ticketing.worker :as worker]))

(defn get-booking-status [db-spec booking-id]
  (booking/get-booking-status db-spec booking-id))

(defn make-payment [db-spec message-queue booking-id]
  (worker/add-book-ticket-request-to-queue message-queue {:booking-id booking-id}))

(defn cancel-booking [db-spec message-queue booking-id]
  (worker/add-cancel-ticket-request-to-queue message-queue {:booking-id booking-id}))
