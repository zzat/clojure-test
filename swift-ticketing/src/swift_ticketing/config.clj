(ns swift-ticketing.config 
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config [cfg-path]
  (-> cfg-path
      (io/resource)
      (aero/read-config)))

(defn read-app-config [] (read-config "config.edn"))
(defn read-test-config [] (read-config "config.test.edn"))
