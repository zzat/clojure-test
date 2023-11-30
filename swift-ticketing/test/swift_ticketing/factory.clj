(ns swift-ticketing.factory)

(defn random-date []
  (apply format "%4d-%02d-%02d" [(+ 2023 (rand-int 5))
                                 (inc (rand-int 12))
                                 (inc (rand-int 28))]))
(defn event-request []
  (let [random-id (rand-int 10000)]
    {"name" (str "Event-" random-id)
     "description" (str "Event-" random-id " Description")
     "date" (random-date)
     "venue" (str "Event-" random-id " Venue")}))
