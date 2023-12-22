(ns swift-ticketing.client
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [swift-ticketing.fixtures :as fixtures]
   [swift-ticketing.factory :as factory]))

(defn- uid-cookie [uid]
  {"uid" {:path "/"
          :value (str uid)}})

(defn- mk-url [server-spec url]
  (str "http://localhost:"
       (:port server-spec)
       url))

(defn- post-request [url body]
  (let [{:keys [server-spec test-user-id]} fixtures/test-env]
    (http/post (mk-url server-spec url) {:form-params body
                                         :content-type :json
                                         :cookies (uid-cookie test-user-id)
                                         :throw-exceptions false})))

(defn- get-request
  ([url] (get-request url {}))
  ([url query-params]
   (let [{:keys [server-spec test-user-id]} fixtures/test-env]
     (http/get (mk-url server-spec url) {:query-params query-params
                                         :cookies (uid-cookie test-user-id)
                                         ; :as :json

                                         :throw-exceptions false}))))

(defn- response-to-json [response]
  (-> response
      :body
      json/read-str))

(defn create-event
  ([] (create-event (factory/event-request)))
  ([request]
   (let [response (post-request "/event" request)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn reserve-general-ticket
  [request]
  (let [response (post-request "/event" request)]
    {:request request
     :status (:status response)
     :response (response-to-json response)}))

(defn list-events
  ([] (list-events {}))
  ([query-params]
   (let [response (get-request "/event" query-params)]
     {:query-params query-params
      :status (:status response)
      :response (response-to-json response)})))

(defn get-event
  ([event-id]
   (let [response (get-request (str "/event/" event-id))]
     {:path-params event-id
      :status (:status response)
      :response (response-to-json response)})))

(defn create-tickets
  ([event-id request]
   (let [response (post-request (str "/event/" event-id "/ticket") request)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn create-general-tickets
  ([event-id] (create-tickets event-id (factory/general-ticket-request))))

(defn create-seated-tickets
  ([event-id] (create-tickets event-id (factory/seated-ticket-request))))

(defn reserve-ticket
  ([event-id request]
   (let [response (post-request (str "/event/" event-id "/booking") request)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn make-payment
  ([booking-id]
   (let [response (post-request (str "/booking/" booking-id "/payment") nil)]
     {:path-params booking-id
      :status (:status response)
      :response (response-to-json response)})))

(defn get-booking-status
  ([booking-id]
   (let [response (get-request (str "/booking/" booking-id "/status"))]
     {:path-params booking-id
      :status (:status response)
      :response (response-to-json response)})))
