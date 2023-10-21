(ns subreddit-monitor.reddit.utils
  (:require [subreddit-monitor.reddit.types :as types]))

(defn make-post [post]
  (let [post-id (get post "id")
        author (get post "author")
        upvotes (get post "ups")
        subreddit (get post "subreddit")]
    (types/map->Post {:id post-id
                      :author author
                      :upvotes upvotes
                      :subreddit subreddit})))

(defn make-comment [comment]
  (let [comment-id (get comment "id")
        author (get comment "author")
        upvotes (get comment "ups")
        subreddit (get comment "subreddit")]
    (types/map->Comment {:id comment-id
                         :author author
                         :upvotes upvotes
                         :subreddit subreddit})))
