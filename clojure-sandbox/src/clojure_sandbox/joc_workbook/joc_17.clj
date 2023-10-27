(ns clojure-sandbox.joc-workbook.joc-17 
  (:require
    [clojure.set :as ra]))

(require '[clojure.set :as ra])

(def artists
  #{{:artist "Burial"  :genre-id 1}
    {:artist "Magma" :genre-id 2}
    {:artist "Can" :genre-id 3}
    {:artist "Faust" :genre-id 3}
    {:artist "Ikonika" :genre-id 1}
    {:artist "Grouper"}})

(def genres
  #{{:genre-id 1 :genre-name "Dubstep"}
    {:genre-id 2 :genre-name "Zeuhl"}
    {:genre-id 3 :genre-name "Prog"}
    {:genre-id 4 :genre-name "Drone"}})

(def ALL identity)
(ra/select ALL genres)

(ra/select (fn [m] (#{1 3} (:genre-id m))) genres)
(defn ids [& ids]
  (fn [m] ((set ids) (:genre-id m))))

(ra/select (ids 1 3) genres)

(ra/select ALL (ra/join artists genres))
