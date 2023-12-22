(ns swift-ticketing.components.worker
  (:require [com.stuartsierra.component :as component]
            [swift-ticketing.worker :as w]
            [clojure.core.async :as async]))

(defrecord Worker [total-workers redis-opts database]
  component/Lifecycle

  (start [component]
    (println ";; Spawn workers")
    (let [connection (:connection database)
          message-queue (async/chan)
          exit-ch (async/chan)]
      (dotimes [i total-workers]
        (w/process-ticket-requests i message-queue connection redis-opts exit-ch))
      (assoc component
             :message-queue message-queue
             :worker-exit-ch exit-ch)))

  (stop [component]
    (println ";; Stop workers")
    (dotimes [i total-workers]
      (async/put! (:worker-exit-ch component) true))
    (async/close! (:message-queue component))
    (dissoc component :message-queue)))

(defn new-worker [total-workers redis-opts]
  (map->Worker {:total-workers total-workers
                :redis-opts redis-opts}))
