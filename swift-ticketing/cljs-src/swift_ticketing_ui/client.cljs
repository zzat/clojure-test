(ns swift-ticketing-ui.client
  (:require
   [ajax.core :as ajax]
   [swift-ticketing-ui.config :refer [API_URL]]))

(defn http-get
  ([url handler]
   (http-get url handler {}))
  ([url handler params]
   (ajax/ajax-request
    {:uri (str API_URL url)
     :method :get
     :handler handler
     :params params
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})})))

(defn http-post [url handler params]
  (ajax/ajax-request
   {:uri (str API_URL url)
    :method :post
    :handler handler
    :params params
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))
