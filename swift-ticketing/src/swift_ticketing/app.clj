(ns swift-ticketing.app
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [swift-ticketing.handlers :as handlers]))

(defn init-routes [db-spec] (defroutes app-routes
                              (GET "/" [] "Hello World")
                              (POST "/event" {:keys [body cookies]}
                                (handlers/create-event db-spec (get-in cookies ["uid" :value]) body))
                              (route/not-found "Not Found")))

(defn swift-ticketing-app [db-spec]
  (-> (init-routes db-spec)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-json-response
      (wrap-json-body {:keywords? true :bigdecimals? true})))

