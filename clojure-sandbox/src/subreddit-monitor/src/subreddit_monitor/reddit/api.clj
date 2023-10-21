(ns subreddit-monitor.reddit.api
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(def reddit-base-url "https://api.reddit.com")
(defn http-get [& args]
  ; (Thread/sleep 10000)
  (apply client/get args))

;; api to fetch all posts from a subreddit 
(defn fetch-subreddit-posts! [subreddit]
  (let [api-url (str reddit-base-url "/r/" subreddit "/new/?limit=10")
        response (http-get api-url)]
    (println "Got posts!")
    (if (= (:status response 200) 200)
      (let [posts (map #(get % "data")
                       (-> response
                           :body
                           json/read-str
                           (get-in ["data" "children"])))]
        posts)
      nil)))

; (fetch-subreddit-posts! "clojure")
;; api to fetch all comments from a post in subreddit 

(defn fetch-post-comments! [subreddit post-id]
  (let [api-url (str reddit-base-url "/r/" subreddit "/comments/" post-id "?limit=10")
        response (http-get api-url)]
    (println api-url)
    (if (= (:status response 200) 200)
      (let [comments (map #(get % "data")
                          (->> response
                               :body
                               json/read-str
                               (mapcat #(get-in % ["data" "children"]))))]
        comments)
      nil)))
; (fetch-post-comments! "17c2i3q")
