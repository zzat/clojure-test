(ns swift-ticketing.worker
  (:require [clojure.core.async :as async]
            [swift-ticketing.db.ticket :as db-ticket]
            [swift-ticketing.db.booking :as db-booking]
            [next.jdbc :as jdbc]
            [clojure.core :as c]
            [swift-ticketing.redis :as redis]
            [taoensso.timbre :as log])
  (:import [java.time Instant Duration]))

(def reserve-event "RESERVE")
(def book-event "BOOK")
(def cancel-event "CANCEL")

(defn- add-ticket-request-to-queue [message-queue request]
  (async/go (async/>! message-queue request)))

(defn request-ticket-reservation
  [message-queue
   {:keys [booking-id ticket-ids ticket-type-id quantity]}]
  (let [event-type reserve-event]
    (add-ticket-request-to-queue
     message-queue
     {:event event-type
      :data {:booking-id booking-id
             :ticket-ids ticket-ids
             :ticket-type-id ticket-type-id
             :quantity quantity}})))

(defn request-ticket-booking [message-queue request]
  (let [event-type book-event
        booking-id (:booking-id request)]
    (add-ticket-request-to-queue
     message-queue
     {:event event-type
      :data {:booking-id booking-id}})))

(defn request-ticket-cancellation [message-queue request]
  (let [event-type cancel-event
        booking-id (:booking-id request)]
    (add-ticket-request-to-queue
     message-queue
     {:event event-type
      :data {:booking-id booking-id}})))

(defn- select-unbooked-tickets-with-db-lock [tx ticket-ids ticket-type-id ticket-quantity]
  (let [selected-rows (db-ticket/lock-unbooked-tickets tx ticket-ids ticket-type-id ticket-quantity)
        selected-ticket-ids (map #(:ticket-id %) selected-rows)
        reservation-timelimit-seconds (:reservation-timelimit-seconds
                                       (first selected-rows))]
    {:locked-ticket-ids selected-ticket-ids
     :reservation-timelimit-seconds reservation-timelimit-seconds}))

(defn- select-unbooked-tickets-with-redis-lock [redis-opts tx ticket-ids ticket-type-id ticket-quantity]
  (let [selected-rows (db-ticket/select-unbooked-tickets tx false ticket-ids ticket-type-id ticket-quantity)
        selected-ticket-ids (map #(:ticket-id %) selected-rows)
        reservation-timelimit-seconds (:reservation-timelimit-seconds (first selected-rows))
        reduction-fn (fn [ticket-id-set ticket-id]
                       (if (nil? (redis/acquire-lock redis-opts ticket-id reservation-timelimit-seconds))
                         ticket-id-set
                         (conj ticket-id-set ticket-id)))
        locked-ticket-ids (reduce reduction-fn #{} selected-ticket-ids)]
    {:locked-ticket-ids locked-ticket-ids
     :reservation-timelimit-seconds reservation-timelimit-seconds}))

(defn- make-lock-unbooked-tickets-fn [redis-opts tx]
  (cond
    (nil? redis-opts) (partial select-unbooked-tickets-with-db-lock tx)
    :else (partial select-unbooked-tickets-with-redis-lock redis-opts tx)))

(defn- make-unlock-unbooked-tickets-fn [redis-opts]
  (cond
    (nil? redis-opts) (constantly nil)
    :else (partial redis/release-lock redis-opts)))

(defn- handle-reserve-event
  [db-spec
   redis-opts
   {{:keys [booking-id ticket-ids ticket-type-id quantity]} :data}]
  (let [requested-quantity quantity
        nil-to-zero (fn [x] (if (nil? x) 0 x))
        ticket-quantity (if (nil? ticket-ids)
                          (nil-to-zero requested-quantity)
                          (count ticket-ids))]
    (if (zero? ticket-quantity)
      (db-booking/update-booking-status db-spec booking-id db-booking/rejected)
      (jdbc/with-transaction [tx db-spec]
        (let [lock-unbooked-tickets (make-lock-unbooked-tickets-fn redis-opts tx)
              release-lock (make-unlock-unbooked-tickets-fn redis-opts)
              {:keys [locked-ticket-ids
                      reservation-timelimit-seconds]}
              (lock-unbooked-tickets ticket-ids ticket-type-id ticket-quantity)
              quantity-available? (and (pos? (count locked-ticket-ids))
                                       (= (count locked-ticket-ids) ticket-quantity))
              booking-status (if quantity-available? db-booking/payment-pending db-booking/rejected)
              current-time (Instant/now)
              reservation-expiration-time (if (nil? reservation-timelimit-seconds)
                                            nil
                                            (.plus current-time
                                                   (Duration/ofSeconds reservation-timelimit-seconds)))]
          (when quantity-available?
            (db-ticket/reserve-tickets tx locked-ticket-ids booking-id reservation-expiration-time))
          (db-booking/update-booking-status tx booking-id booking-status)
          (map #(release-lock %) locked-ticket-ids))))))

(defn- handle-book-event [db-spec request]
  (jdbc/with-transaction [tx db-spec]
    (let [booking-id (get-in request [:data :booking-id])
          selected-ticket-ids (->> (db-ticket/lock-reserved-tickets tx booking-id)
                                   (map :ticket-id))]
      (if (empty? selected-ticket-ids)
        (db-booking/update-booking-status tx booking-id db-booking/rejected)
        (do
          (db-ticket/confirm-tickets tx selected-ticket-ids)
          (db-booking/update-booking-status tx booking-id db-booking/confirmed))))))

(defn- handle-cancel-event [db-spec request]
  (jdbc/with-transaction [tx db-spec]
    (let [booking-id (get-in request [:data :booking-id])
          selected-ticket-ids (->> (db-ticket/lock-reserved-tickets tx booking-id)
                                   (map :ticket-id))]
      (db-ticket/cancel-tickets tx selected-ticket-ids)
      (db-booking/update-booking-status tx booking-id db-booking/canceled))))

(defn- process-ticket-request* [worker-id db-spec redis-opts request]
  (try
    (let [event-type (:event request)]
      (log/debug "Got Message:" request)
      (cond
        (= event-type reserve-event)
        (handle-reserve-event db-spec redis-opts request)
        (= event-type book-event)
        (handle-book-event db-spec request)
        (= event-type cancel-event)
        (handle-cancel-event db-spec request)
        :else (log/error "Worker: Unknown event"))
      :continue)
    (catch Exception e
      (log/error "Exception in Worker: " worker-id " :" e))))

(defn process-ticket-requests [worker-id message-queue db-spec redis-opts exit-ch]
  (async/go-loop []
    (cond
      (= :continue
         (async/alt!
           message-queue ([request]
                          (process-ticket-request* worker-id db-spec redis-opts request))
           exit-ch :exit)) (recur)
      :else nil)))
