(ns swift-ticketing.app
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [swift-ticketing.handlers :as handlers]))

(defn init-routes [db-spec]
  (defroutes app-routes
    (GET "/" [] "Hello World")
    (GET "/event" [venue from to]
      (handlers/get-event db-spec venue from to))
    (POST "/event" {:keys [body cookies]}
      (handlers/create-event db-spec (get-in cookies ["uid" :value]) body))
    (POST "/event/:event-id/ticket" {:keys [body cookies route-params]}
      (handlers/create-tickets db-spec (get-in cookies ["uid" :value]) (:event-id route-params) body))
    (route/not-found "Not Found")))

(defn swift-ticketing-app [db-spec]
  (-> (init-routes db-spec)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true :bigdecimals? true})))

