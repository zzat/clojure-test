(ns swift-ticketing.factory
  (:require
   [next.jdbc :as jdbc]
   [honey.sql :as sql]))

(defn mk-prefix [x]
  (str x "-"))

(defn mk-event-name [x]
  (str (mk-prefix "Event") x))

(defn mk-event-description [x]
  (str (mk-event-name x) " Description"))

(defn mk-event-venue [x]
  (str (mk-event-name x) " Venue"))

(defn mk-ticket-type [x]
  (str (mk-prefix "TicketType") x))

(defn mk-ticket-name [x]
  (str (mk-prefix "Ticket") x))

(defn mk-ticket-description [x]
  (str (mk-ticket-name x) " Description"))

(defn random-date []
  (apply format "%4d-%02d-%02d" [(+ 2023 (rand-int 5))
                                 (inc (rand-int 12))
                                 (inc (rand-int 28))]))

(defn event-request []
  (let [random-id (rand-int 10000)]
    {"name" (mk-event-name random-id)
     "description" (mk-event-description random-id)
     "date" (random-date)
     "venue" (mk-event-venue random-id)}))

(defn event-with-tickets [event-id]
  (let [ticket-type-id (java.util.UUID/randomUUID)]
    {:event_id (str event-id)
     :event_name (mk-event-name event-id)
     :event_description (mk-event-description event-id)
     :event_date (random-date)
     :venue (mk-event-venue event-id)
     :ticket_type (mk-ticket-type event-id)
     :ticket_type_id (str ticket-type-id)
     :seat_type "General"
     :ticket_count (rand-int 100)
     :ticket_name (mk-ticket-name event-id)
     :ticket_description (mk-ticket-description event-id)
     :ticket_price (rand-int 10000)}))

(defn general-ticket-request []
  (let [random-id (rand-int 10000)]
    {"ticket_type" (mk-ticket-type random-id)
     "description" (mk-ticket-description random-id)
     "seat_type" "General"
     "quantity" (inc (rand-int 1000))
     "price" (inc (rand-int 2000))}))

(defn seated-ticket-request []
  (let [random-id (rand-int 10000)
        seat (fn [x] {"name" (str x)})]
    {"ticket_type" (mk-ticket-type random-id)
     "description" (mk-ticket-description random-id)
     "seat_type" "Named"
     "reservation_limit_in_seconds" (+ 50 (rand-int 200))
     "seats" (for [x (range (inc (rand-int 20)))] (seat x))
     "price" (inc (rand-int 2000))}))

(defn mk-reserve-general-ticket-request [quantity ticket-type-id]
  {"quantity" quantity
   "ticket_type_id" ticket-type-id})

(defn mk-reserve-seated-ticket-request [ticket-ids]
  {"ticket_ids" ticket-ids})

(defn add-user-table-entry [db-spec]
  (let [user-id (java.util.UUID/randomUUID)
        insert-user (sql/format {:insert-into :user_account
                                 :columns [:user_id :name]
                                 :values [[user-id
                                           "Test User"]]})]
    (jdbc/execute! db-spec insert-user)
    user-id))
