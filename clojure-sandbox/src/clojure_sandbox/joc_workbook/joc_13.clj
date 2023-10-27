(ns clojure-sandbox.joc-workbook.joc-13
  (:require [goog.structs.LinkedMap]))

(extend-type goog.structs.LinkedMap
  cljs.core.ICounted
  (-count [m] (.getCount m)))
