(ns subreddit-monitor.db.query
  (:require [honey.sql :as sql]))

(defn select-author [subreddit table order limit]
  (sql/format {:select [:*, :%count.*]
               :from [table]
               :where [:= :subreddit subreddit]
               :group-by :author
               :order-by [[:%count.* order]]
               :limit limit} {:inline true}))

(defn select-top-poster [subreddit]
  (select-author subreddit "post" :desc 1))

(defn select-top-commentor [subreddit]
  (select-author subreddit "comment" :desc 1))

(comment (select-top-poster "Clojure"))
