(ns swift-ticketing.components.worker
  (:require [com.stuartsierra.component :as component]
            [swift-ticketing.worker :as w]))

(defrecord Worker [total-workers redis-opts database]
  component/Lifecycle

  (start [component]
    (println ";; Spawn worker army")
    (let [connection (:connection database)
          mk-worker-thread (fn [_]
                             (Thread.
                              #(w/process-ticket-requests
                                connection redis-opts)))
          worker-threads (map mk-worker-thread
                              (range total-workers))]
      (doseq [worker-thread worker-threads]
        (.start worker-thread))
      (assoc component :workers worker-threads)))

  (stop [component]
    (println ";; Slay workers")
    (dotimes [worker-thread (:workers component)]
      (.stop worker-thread))
    (assoc component :workers nil)))

(defn new-worker [total-workers redis-opts]
  (map->Worker {:total-workers total-workers
                :redis-opts redis-opts}))
