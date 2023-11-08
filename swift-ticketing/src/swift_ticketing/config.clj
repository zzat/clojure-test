(ns swift-ticketing.config 
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config [cfg-path]
  (-> cfg-path
      (io/resource)
      (aero/read-config)))
