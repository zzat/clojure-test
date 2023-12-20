(ns swift-ticketing.components.http-server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [swift-ticketing.app :as app]))

(defrecord HTTPServer [port database worker]
  component/Lifecycle

  (start [component]
    (println ";; Starting API Server")
    (let [connection (:connection database)
          message-queue (:message-queue worker)
          server (run-jetty
                  (app/swift-ticketing-app connection message-queue)
                  {:port port
                   :join? true})]
      (assoc component :http-server server)))

  (stop [component]
    (println ";; Stopping API Server")
    (.stop (:http-server component))
    (assoc component :http-server nil)))

(defn new-http-server [port]
  (map->HTTPServer {:port port}))
