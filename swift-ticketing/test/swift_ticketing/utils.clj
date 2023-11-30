(ns swift-ticketing.utils
  (:require [clojure.set :as s]))

(defn submap? [m1 m2]
  (s/subset? (set m1) (set m2)))

(defn format-json-date [date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
           (.parse
            (java.text.SimpleDateFormat.
             "yyyy-MM-dd'T'HH:mm:ssX")
            date)))
