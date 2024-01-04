(ns swift-ticketing.app
  (:require
   [compojure.route :as route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [swift-ticketing.handlers :as handlers]
   [compojure.api.sweet :refer [defroutes GET POST]]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]))

(defn init-routes [db-spec message-queue]
  (defroutes app-routes
    (GET "/event" request
      (handlers/get-events-handler db-spec request))
    (POST "/event" request
      (handlers/create-event-handler db-spec request))
    (GET "/event/:event-id" request
      (handlers/get-event-handler db-spec request))
    (POST "/event/:event-id/ticket" request
      (handlers/create-tickets-handler db-spec request))
    (POST "/event/:event-id/booking" request
      (handlers/reserve-ticket-handler db-spec message-queue request))
    (GET "/ticket" request
      (handlers/get-tickets-handler db-spec request))
    (GET "/booking/:booking-id/status" request
      (handlers/get-booking-status-handler db-spec request))
    (POST "/booking/:booking-id/payment" request
      (handlers/post-payment-handler message-queue request))
    (POST "/booking/:booking-id/cancel" request
      (handlers/cancel-booking-handler message-queue request))
    (GET "/booking/:booking-id/ticket" request
      (handlers/get-tickets-by-booking-id-handler db-spec request))
    (route/not-found "Not Found")))

(defn- wrap-response-kebab [handler]
  (fn [request]
    (let [response (handler request)]
      (update response
              :body
              (partial cske/transform-keys csk/->snake_case_keyword)))))

(defn- wrap-request-kebab [handler]
  (fn [request]
    (handler
     (update request
             :body-params
             (partial cske/transform-keys csk/->kebab-case-keyword)))))

(defn swift-ticketing-app [db-spec message-queue]
  (-> (init-routes db-spec message-queue)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-json-body {:keywords? true
                       :bigdecimals? true})
      wrap-request-kebab
      wrap-response-kebab
      wrap-json-response))
