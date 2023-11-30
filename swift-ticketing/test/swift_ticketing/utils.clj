(ns swift-ticketing.utils
  (:require [clojure.set :as s]))

(defn submap? [m1 m2]
  (s/subset? (set m1) (set m2)))
