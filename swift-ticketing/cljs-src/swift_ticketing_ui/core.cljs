(ns swift-ticketing-ui.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [secretary.core :as secretary]
   [accountant.core :as accountant]
   [swift-ticketing-ui.event :as event]
   [swift-ticketing-ui.ticket :as ticket]
   [swift-ticketing-ui.booking :as booking]))

;; -------------------------
;; Views

(defn start []
  (secretary/dispatch! "/event"))

(defn home-page []
  [:div.bg-gray-50
   [:main
    [:div {:class "mx-auto max-w-3xl px-4 sm:px-6 lg:max-w-7xl lg:px-8"}
     [:button {:on-click start} "Event"]]]])

;; Routes
(secretary/defroute "/" []
  (d/render [home-page] (.getElementById js/document "app")))

(secretary/defroute "/event" []
  (d/render [event/events-page] (.getElementById js/document "app")))

(secretary/defroute "/event/create" []
  (d/render [event/event-form] (.getElementById js/document "app")))

(secretary/defroute "/event/:event-id" [event-id]
  (d/render [event/event-page event-id] (.getElementById js/document "app")))

(secretary/defroute "/event/:event-id/ticket/create" [event-id]
  (d/render [ticket/ticket-form event-id] (.getElementById js/document "app")))

(secretary/defroute "/event/:event-id/ticket/:ticket-type-id" {:as params}
  (let []
    (d/render [ticket/seats-page (:event-id params) (:ticket-type-id params)] (.getElementById js/document "app"))))

(secretary/defroute "/booking/:booking-id" [booking-id]
  (d/render [booking/booking-page booking-id] (.getElementById js/document "app")))

(secretary/defroute "/booking/payment/:booking-id" [booking-id]
  (d/render [booking/payment-page booking-id] (.getElementById js/document "app")))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [event/events-page] (.getElementById js/document "app")))

; (defn ^:export init! []
;   (mount-root))

(defn ^:export init! []
  (accountant/configure-navigation!
   {:nav-handler   (fn [path] (secretary/dispatch! path))
    :path-exists?  (fn [path] (secretary/locate-route path))})
  (accountant/dispatch-current!))
