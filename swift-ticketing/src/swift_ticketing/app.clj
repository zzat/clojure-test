(ns swift-ticketing.app
  (:require ; [compojure.core :refer :all]
   [compojure.route :as route]
   ; [reitit.ring :as ring]
   ; [reitit.spec :as rs]
   [clojure.spec.alpha :as s]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [swift-ticketing.handlers :as handlers]
   [compojure.api.sweet :as compojure]
   [compojure.api.sweet :refer :all]
   ; [schema.core :as s]
            ; [swift-ticketing.models :as models]

   [swift-ticketing.app :as app]))

; (s/def ::event:venue int?)
; (s/def ::event:to string?)
; (s/def ::event:from string?)

(defn init-routes [db-spec]
  (defroutes app-routes
    (GET "/" [] "Hello World")
    (GET "/event" [venue from to]
         ; :query-params [venue :- ::event:venue, from :- ::event:from, to :- ::event:to]
      (handlers/get-events-handler db-spec venue from to))
    (POST "/event" {:keys [body cookies]}
      (handlers/create-event-handler db-spec (get-in cookies ["uid" :value]) body))
    (GET "/event/:event-id" {:keys [route-params]}
      (handlers/get-event-handler db-spec (:event-id route-params)))
    (POST "/event/:event-id/ticket" {:keys [body cookies route-params]}
      (handlers/create-tickets-handler db-spec (get-in cookies ["uid" :value]) (:event-id route-params) body))
    (POST "/event/:event-id/booking" {:keys [body cookies route-params]}
      (handlers/book-ticket-handler db-spec (get-in cookies ["uid" :value]) (:event-id route-params) body))
    (GET "/ticket" [ticket_type_id]
      (handlers/get-tickets-handler db-spec ticket_type_id))
    (GET "/booking/:booking-id/status" {:keys [cookies route-params]}
      (handlers/get-booking-status-handler db-spec (get-in cookies "uid" :value) (:booking-id route-params)))
    (POST "/booking/:booking-id/payment" {:keys [cookies route-params]}
      (handlers/post-payment-handler db-spec (:booking-id route-params)))
    (POST "/booking/:booking-id/cancel" {:keys [cookies route-params]}
      (handlers/cancel-booking-handler db-spec (:booking-id route-params)))
    (route/not-found "Not Found")))

(defn swift-ticketing-app [db-spec]
  (-> (init-routes db-spec)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true :bigdecimals? true})))

