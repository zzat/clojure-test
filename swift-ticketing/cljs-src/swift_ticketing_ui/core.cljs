(ns swift-ticketing-ui.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [secretary.core :as secretary]
   [swift-ticketing-ui.event :as event]
   ))

;; -------------------------
;; Views

(defn start []
  (secretary/dispatch! "/event"))

(defn home-page []
  [:div.bg-gray-50
   [:main
    [:div {:class "mx-auto max-w-3xl px-4 sm:px-6 lg:max-w-7xl lg:px-8"}
     [:button {:on-click start} "Event"]
     ]]])

;; Routes
(secretary/defroute "/" []
  (d/render [home-page] (.getElementById js/document "app")))

(secretary/defroute "/event" []
  (d/render [event/event-form] (.getElementById js/document "app")))

(secretary/defroute "/event/create" []
  (d/render [event/event-form] (.getElementById js/document "app")))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root)
  )
