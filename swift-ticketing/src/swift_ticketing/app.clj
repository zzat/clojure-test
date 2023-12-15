(ns swift-ticketing.app
  (:require
   [compojure.route :as route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [swift-ticketing.handlers :as handlers]
   [compojure.api.sweet :as compojure]
   [compojure.api.sweet :refer :all]
   [swift-ticketing.app :as app]))

(defn init-routes [db-spec]
  (let [get-uid (fn [cookies]
                  (get-in cookies ["uid" :value]))]
    (defroutes app-routes
      (GET "/" [] "Hello World")
      (GET "/event" [venue from to]
        (handlers/get-events-handler db-spec venue from to))
      (POST "/event" {:keys [body cookies]}
        (handlers/create-event-handler db-spec (get-uid cookies) body))
      (GET "/event/:event-id" {:keys [route-params]}
        (handlers/get-event-handler db-spec (:event-id route-params)))
      (POST "/event/:event-id/ticket" {:keys [body cookies route-params]}
        (handlers/create-tickets-handler db-spec (get-uid cookies) (:event-id route-params) body))
      (POST "/event/:event-id/booking" {:keys [body cookies route-params]}
        (handlers/reserve-ticket-handler db-spec (get-uid cookies) (:event-id route-params) body))
      (GET "/ticket" [ticket_type_id]
        (handlers/get-tickets-handler db-spec ticket_type_id))
      (GET "/booking/:booking-id/status" {:keys [cookies route-params]}
        (handlers/get-booking-status-handler db-spec (:booking-id route-params)))
      (POST "/booking/:booking-id/payment" {:keys [cookies route-params]}
        (handlers/post-payment-handler db-spec (:booking-id route-params)))
      (POST "/booking/:booking-id/cancel" {:keys [cookies route-params]}
        (handlers/cancel-booking-handler db-spec (:booking-id route-params)))
      (GET "/booking/:booking-id/ticket" {:keys [cookies route-params]}
        (handlers/get-tickets-by-booking-id-handler db-spec (:booking-id route-params)))
      (route/not-found "Not Found"))))

(defn swift-ticketing-app [db-spec]
  (-> (init-routes db-spec)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true 
                       :bigdecimals? true})))
