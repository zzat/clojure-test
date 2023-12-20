(ns swift-ticketing.app
  (:require
   [compojure.route :as route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [swift-ticketing.handlers :as handlers]
   [compojure.api.sweet :as compojure]
   [compojure.api.sweet :refer :all]
   [swift-ticketing.app :as app]))

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
      (handlers/post-payment-handler db-spec message-queue request))
    (POST "/booking/:booking-id/cancel" request
      (handlers/cancel-booking-handler db-spec message-queue request))
    (GET "/booking/:booking-id/ticket" request
      (handlers/get-tickets-by-booking-id-handler db-spec request))
    (route/not-found "Not Found")))

(defn swift-ticketing-app [db-spec message-queue]
  (-> (init-routes db-spec message-queue)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true
                       :bigdecimals? true})))
