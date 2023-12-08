(ns swift-ticketing.db.booking
  (:require [honey.sql :as sql]
            [swift-ticketing.db.query :refer [run-query]]))

(defonce INPROCESS "InProcess")
(defonce CONFIRMED "Confirmed")
(defonce PAYMENTPENDING "PaymentPending")
(defonce REJECTED "Rejected")
(defonce CANCELED "Canceled")

(defn insert-booking-query [uid booking-id]
  (sql/format {:insert-into :booking
               :columns [:booking_id :user_id :booking_status]
               :values [[booking-id
                         [:cast uid :uuid]
                         [:cast INPROCESS :booking_status]]]}))

(defn insert-booking [db-spec & args]
  (run-query db-spec insert-booking-query args))

(defn get-booking-status-query [uid booking-id]
  (sql/format {:select [:booking_status] :from :booking
               :where [[:= :booking_id [:cast booking-id :uuid]]]}))

(defn get-booking-status [db-spec & args]
  (run-query db-spec get-booking-status-query args))

(defn update-booking-status-query [booking-id booking-status]
  (sql/format {:update :booking
               :set {:booking_status [:cast booking-status :booking_status]}
               :where [:= :booking_id [:cast booking-id :uuid]]}))

(defn update-booking-status [db-spec & args]
  (run-query db-spec update-booking-status-query args))
