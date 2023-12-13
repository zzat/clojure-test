(ns swift-ticketing.client
  (:require
   [ring.mock.request :as mock]
   [clojure.data.json :as json]
   [swift-ticketing.app :refer [swift-ticketing-app]]
   [swift-ticketing.fixtures :as fixtures]
   [swift-ticketing.factory :as factory]))

(defn- post-request [url body]
  (-> (mock/request :post url)
      (mock/json-body body)))

(defn- get-request
  ([url] (get-request url nil))
  ([url query-params]
   (-> (mock/request :get url)
       ((fn [r] (if (nil? query-params)
                  (identity r)
                  (mock/query-string r query-params)))))))

(defn- response-to-json [response]
  (-> response
      :body
      json/read-str))

(defn create-event
  ([] (create-event (factory/event-request)))
  ([request]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (post-request "/event" request)
                      (mock/cookie "uid" test-user-id)
                      app)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn reserve-general-ticket
  [request]
  (let [{:keys [db-spec test-user-id]} fixtures/test-env
        app (fn [req] ((swift-ticketing-app db-spec) req))
        response (-> (post-request "/event" request)
                     (mock/cookie "uid" test-user-id)
                     app)]
    (println response)
    {:request request
     :status (:status response)
     :response (response-to-json response)}))

(defn list-events
  ([] (list-events {}))
  ([query-params]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (get-request "/event" query-params)
                      app)]
     {:query-params query-params
      :status (:status response)
      :response (response-to-json response)})))

(defn get-event
  ([event-id]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (get-request (str "/event/" event-id))
                      app)]
     {:path-params event-id
      :status (:status response)
      :response (response-to-json response)})))

(defn create-tickets
  ([event-id request]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (post-request (str "/event/" event-id "/ticket") request)
                      (mock/cookie "uid" test-user-id)
                      app)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn create-general-tickets
  ([event-id] (create-tickets event-id (factory/general-ticket-request))))

(defn create-seated-tickets
  ([event-id] (create-tickets event-id (factory/seated-ticket-request))))

(defn reserve-ticket
  ([event-id request]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (post-request (str "/event/" event-id "/booking") request)
                      (mock/cookie "uid" test-user-id)
                      app)]
     {:request request
      :status (:status response)
      :response (response-to-json response)})))

(defn make-payment
  ([booking-id]
   (let [{:keys [db-spec test-user-id]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (post-request (str "/booking/" booking-id "/payment") nil)
                      (mock/cookie "uid" test-user-id)
                      app)]
     {:path-params booking-id
      :status (:status response)
      :response (response-to-json response)})))

(defn get-booking-status
  ([booking-id]
   (let [{:keys [db-spec]} fixtures/test-env
         app (fn [req] ((swift-ticketing-app db-spec) req))
         response (-> (get-request (str "/booking/" booking-id "/status"))
                      app)]
     {:path-params booking-id
      :status (:status response)
      :response (response-to-json response)})))
