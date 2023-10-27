(ns clojure-sandbox.joc-workbook.joc-11
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip])
  (:import (java.util.regex Pattern)
           (java.util.concurrent Executors)))

(time (let [x (future (Thread/sleep 2000) (+ 41 1))]
        [@x @x]))

(defn feed->zipper [url]
  (->> (xml/parse url)
       (zip/xml-zip)))

(defn normalize [feed]
  (if (= :feed (:tag (first feed)))
    feed (zip/down feed)))

(defn feed-children [uri-str]
  (->> uri-str
       feed->zipper
       normalize
       zip/children
       (filter (comp #{:item :entry} :tag))))

(defn title [entry]
  (some->> entry
           :content
           :content
           first))

(defn count-text-task [extractor txt feed]
  (let [items (feed-children feed)
        re    (Pattern/compile (str "(?i)" txt))]
    (->> items
         (map extractor) (mapcat #(re-seq re %))
         count)))

(count-text-task title "Erlang" "http://feeds.feedburner.com/ElixirLang")

(def feeds #{"http://feeds.feedburner.com/ElixirLang"
             "http://blog.fogus.me/feed/"})

(let [results (for [feed feeds]
                (future
                  (count-text-task title "Elixir" feed)))]
  (reduce + (map deref results)))

(defmacro as-futures [[a args] & body]
  (let [parts (partition-by #{'=>} body)
        [acts _ [res]] (partition-by #{:as} (first parts))
        [_ _ task]     parts]
    `(let [~res (for [~a ~args] (future  ~@acts))]
       ~@task)))

(defn occurrences [extractor tag & feeds]
  (as-futures [feed feeds]
              (count-text-task extractor tag feed)
              :as results
              =>
              (reduce + (map deref results))))

(apply (partial occurrences title "Elixir") feeds)

(def thread-pool
  (Executors/newFixedThreadPool 4))

(defn dothreads! [f & {thread-count :threads
                       exec-count :times :or {thread-count 1 exec-count 1}}]
  (dotimes [_ thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))

(def x (promise))
(def y (promise))
(def z (promise))

(dothreads! #(deliver z (+ @x @y)))

(dothreads!
 #(do (Thread/sleep 1000) (deliver x 52)))

(dothreads!
 #(do (Thread/sleep 2000) (deliver y 86)))
(time @z)

(defmacro with-promises [[n tasks _ as] & body]
  (when as
    `(let [tasks# ~tasks
           n# (count tasks#)
           promises# (take n# (repeatedly promise))]
       (dotimes [i# n#]
         (dothreads!
          (fn []
            (deliver (nth promises# i#)
                     ((nth tasks# i#))))))
       (let [~n tasks#
             ~as promises#]
         ~@body))))

(defrecord TestRun [run passed failed])
(defn pass [] (Thread/sleep 500) true)
(defn fail [] (Thread/sleep 100) false)

(defn run-tests [& all-tests]
  (with-promises
    [tests all-tests :as results]
    (into (TestRun. 0 0 0)
          (reduce #(merge-with + %1 %2) {}
                  (for [r results]
                    (if @r
                      {:run 1 :passed 1}
                      {:run 1 :failed 1}))))))

(time (run-tests pass fail fail fail pass))

(defn sleeper [s thing] (Thread/sleep (* 1000 s)) thing)
(defn pvs [] (pvalues
              (sleeper 2 :1st)
              (sleeper 3 :2nd)
              (keyword "3rd")))

(-> (pvs) first time)
(-> (pvs) last time)

(->> [1 2 3]
     (pmap (comp inc (partial sleeper 2)))
     doall
     time)

(-> (pcalls
     #(sleeper 2 :first)
     #(sleeper 3 :second)
     #(keyword "3rd"))
    doall
    time)

