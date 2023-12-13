(ns swift-ticketing.db.query
  (:require
   [next.jdbc :as jdbc]
   [honey.sql :as sql]
   [swift-ticketing.fixtures :as fixtures]
   [next.jdbc.result-set :as rs]
   [swift-ticketing.db.booking :as booking]
   [swift-ticketing.db.ticket :as ticket]))

(defn run-query! [q]
  (let [{:keys [db-spec]} fixtures/test-env]
    (jdbc/execute! db-spec q {:builder-fn rs/as-unqualified-maps})))

(defn run-query-one! [q]
  (let [{:keys [db-spec]} fixtures/test-env]
    (jdbc/execute-one! db-spec q {:builder-fn rs/as-unqualified-maps})))

(defn get-booking-status [booking-id]
  (let [{:keys [test-user-id]} fixtures/test-env
        booking-status (-> (run-query-one!
                            (booking/get-booking-status test-user-id booking-id))
                           :booking_status)]
    booking-status))

(defn get-tickets-by-booking-id [booking-id]
  (let [tickets (run-query!
                 (ticket/get-tickets-by-booking-id booking-id))]
    tickets))

(defn get-booking [booking-id]
  (let [booking (run-query-one!
                 (sql/format {:select [:*] :from :booking
                              :where [:= :booking_id booking-id]}))]
    booking))

(defn insert-booking [booking]
  (run-query! (sql/format {:insert-into :booking
                           :columns [:booking_id :user_id :booking_status]
                           :values [[(:booking_id booking)
                                     (:user_id booking)
                                     [:cast (:booking_status booking) :booking_status]]]})))
