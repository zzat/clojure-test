(ns swift-ticketing.redis
  (:require [taoensso.carmine :as car :refer [wcar]]
            [taoensso.timbre :as log]))

(defn acquire-lock [redis-opts lock-key timeout]
  (log/debug "Locking" lock-key)
  (let [lock-id (java.util.UUID/randomUUID)]
    (wcar redis-opts
          (let [ok (car/setnx lock-key lock-id)]
            (when ok
              (car/expire lock-key timeout)
              lock-id)))))

(defn release-lock [redis-opts lock-key]
  (wcar redis-opts
        (car/del lock-key)))
