(ns subreddit-monitor.reddit.types)

(defrecord Post [id author upvotes subreddit])

(defrecord Comment [id author upvotes subreddit])
