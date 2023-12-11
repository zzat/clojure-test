(ns swift-ticketing.db
  (:require
   [next.jdbc :as jdbc]
   [swift-ticketing.fixtures :as fixtures]
   [next.jdbc.result-set :as rs]
   [swift-ticketing.db.booking :as booking]
   [swift-ticketing.db.ticket :as ticket]))

(defn get-booking-status [booking-id]
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        booking-status (-> (jdbc/execute-one!
                            db-spec
                            (booking/get-booking-status test-user-id booking-id)
                            {:builder-fn rs/as-unqualified-maps})
                           :booking_status)]
    booking-status))

(defn get-tickets-by-booking-id [booking-id]
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        tickets (-> (jdbc/execute!
                     db-spec
                     (ticket/get-tickets-by-booking-id booking-id)
                     {:builder-fn rs/as-unqualified-maps}))]
    tickets))
