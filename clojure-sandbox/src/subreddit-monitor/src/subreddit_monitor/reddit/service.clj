(ns subreddit-monitor.reddit.service
  (:require [subreddit-monitor.reddit.api :as api]
            [subreddit-monitor.reddit.utils :as utils]
            [subreddit-monitor.db.dml :as dml]
            [subreddit-monitor.db.ddl :as ddl]
            [subreddit-monitor.db.query :as query]
            [next.jdbc :as jdbc]))

(defn fetch-and-insert-comments [db-spec subreddit post-id]
  (let [comments (->> post-id
                      (api/fetch-post-comments! subreddit)
                      (map utils/make-comment))]
    ; (map #(jdbc/execute! db-spec (dml/insert-post-query %)) posts)
    (jdbc/execute! db-spec (dml/insert-comments-query comments))))

(defn print-stats [db-spec subreddit]
  (println "Stats at: " (.toString (java.util.Date.)))
  (print "Top Poster: ")
  (println (:post/author (first (jdbc/execute! db-spec (query/select-top-poster subreddit)))))
  (print "Top Commentor: ")
  (println (:comment/author (first (jdbc/execute! db-spec (query/select-top-commentor subreddit))))))

(defn scrape-subreddit [db-spec subreddit]
  (let [posts (->> subreddit
                   api/fetch-subreddit-posts!
                   (map utils/make-post))]
    (println "Monitor...")
    (jdbc/execute! db-spec (dml/insert-posts-query posts))
    (map #(fetch-and-insert-comments db-spec subreddit (:id %)) posts)))

(defn init-db [db-spec]
  (jdbc/execute! db-spec ddl/create-subreddit-query)
  (jdbc/execute! db-spec ddl/create-post-query)
  (jdbc/execute! db-spec ddl/create-comment-query))

(comment
  (let [datasource (jdbc/get-datasource
                    {:jdbcUrl "jdbc:sqlite:/Users/zzat/Documents/sample-sqlite-db"})]
    (with-open [conn (jdbc/get-connection datasource)]
      (jdbc/execute! conn (dml/insert-comments-query comments)))))

(comment
  (let [datasource (jdbc/get-datasource
                    {:jdbcUrl "jdbc:sqlite:/Users/zzat/Documents/sample-sqlite-db"})]
    (with-open [conn (jdbc/get-connection datasource)]
      (jdbc/execute! conn (dml/insert-posts-query posts))
      (map #(fetch-and-insert-comments conn subreddit (:id %)) posts)
    ;; (map #(fetch-and-insert-comments conn subreddit (:id %)) [(nth posts 0)])
      )))

(comment
  (let [datasource (jdbc/get-datasource
                    {:jdbcUrl "jdbc:sqlite:/Users/zzat/Documents/sample-sqlite-db"})]
    (with-open [conn (jdbc/get-connection datasource)]
      (jdbc/execute! conn (query/select-top-poster "Clojure")))))
