(ns swift-ticketing.core
  (:require [swift-ticketing.config :as config]
            [taoensso.carmine :as car]
            [com.stuartsierra.component :as component]
            [swift-ticketing.components.db :refer [new-database]]
            [swift-ticketing.components.http-server :refer
             [new-http-server]]
            [swift-ticketing.components.worker :refer [new-worker]])
  (:gen-class))

(defonce redis-conn-pool (car/connection-pool {}))

(defn swift-ticketing-system [config]
  (let [locking-strategy (:locking-strategy config)
        redis-opts (when (= "redis" locking-strategy)
                     {:pool redis-conn-pool
                      :spec (:redis config)})]
    (component/system-map
     :database (new-database (:database config))
     :app (component/using
           (new-http-server (get-in config [:server :port])
                            (get-in config [:server :join?]))
           [:database])
     :worker (component/using
              (new-worker 5 redis-opts)
              [:database]))))

(defn -main
  [& args]
  (let [config (config/read-app-config)]
    (component/start (swift-ticketing-system config))))
