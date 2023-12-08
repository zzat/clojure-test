(ns swift-ticketing.components.worker
  (:require [com.stuartsierra.component :as component]
            [swift-ticketing.worker :as w]))

(defrecord Worker [total-workers redis-opts database]
  component/Lifecycle

  (start [component]
    (println ";; Spawn workers")
    (let [connection (:connection database)]
      (dotimes [i total-workers]
        (w/process-ticket-requests i connection redis-opts)))
    component)

  (stop [component]
    (println ";; Stop workers")
    ; (async/close! w/ticket-queue)
    component))

(defn new-worker [total-workers redis-opts]
  (map->Worker {:total-workers total-workers
                :redis-opts redis-opts}))
