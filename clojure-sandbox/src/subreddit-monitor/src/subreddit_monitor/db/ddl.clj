(ns subreddit-monitor.db.ddl
  (:require [honey.sql :as sql]))

(def create-subreddit-query
  (sql/format {:create-table :subreddit
               :with-columns
               [[:id :text [:not nil]]
                [:name :text]
                [:created_at :timestamptz]
                [:last_scanned_at :timestamptz]]
              }))

(def create-post-query
  (sql/format {:create-table :post
               :with-columns
               [[:id :text [:not nil]]
                [:author :text]
                [:upvotes :int]
                [:subreddit :text]
                [:created_at :timestamptz]]
              }))

(def create-comment-query
  (sql/format {:create-table :comment
               :with-columns
               [[:id :text]
                [:author :text]
                [:upvotes :int]
                [:subreddit :text]
                [:created_at :timestamptz]]
              }))
