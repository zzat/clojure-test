(ns swift-ticketing-ui.config
  (:require [adzerk.env :as env]))

; (def API-URL (or (.-API_URL js/process.env) "API_URL ENV not defined"))
(env/def API_URL :required) 
; (def API_URL _API_URL__)
