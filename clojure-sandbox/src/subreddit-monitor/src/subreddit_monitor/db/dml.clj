(ns subreddit-monitor.db.dml
  (:require [honey.sql :as sql]
            [subreddit-monitor.reddit.types :as types]))

(defn insert-subreddit-query [subreddit]
  (sql/format {:insert-into :subreddit
               :columns [:id :name]
               :values [[(:id subreddit) (:name subreddit)]]}))

(defn insert-post-query [post]
  (sql/format {:insert-into :post
               :columns [:id :author :upvotes :subreddit]
               :values [[(:id post)
                         (:author post)
                         (:upvotes post)
                         (:subreddit post)]]}))

(defn insert-posts-query [posts]
  (let [post-values (map (fn [post] [(:id post)
                                     (:author post)
                                     (:upvotes post)
                                     (:subreddit post)]) posts)]
    (sql/format {:insert-into :post
                 :columns [:id :author :upvotes :subreddit]
                 :values (vec post-values)})))

(defn insert-comment-query [comment]
  (sql/format {:insert-into :comment
               :columns [:id :author :upvotes :subreddit]
               :values [[(:id comment)
                         (:author comment)
                         (:upvotes comment)
                         (:subreddit comment)]]}))

(defn insert-comments-query [comments]
  (let [comment-values (map (fn [comment] [(:id comment)
                                           (:author comment)
                                           (:upvotes comment)
                                           (:subreddit comment)]) comments)]
    (sql/format {:insert-into :comment
                 :columns [:id :author :upvotes :subreddit]
                 :values (vec comment-values)})))
