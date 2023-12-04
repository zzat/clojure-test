(ns swift-ticketing-ui.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [secretary.core :as secretary :include-macros true]
   [accountant.core :as accountant]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [swift-ticketing-ui.event :as event]
   [swift-ticketing-ui.ticket :as ticket]
   [swift-ticketing-ui.booking :as booking])
  (:import [goog History]
           [goog.history EventType]))

;; -------------------------
;; Views

(defn start []
  (accountant/navigate! "/event"))

(defn home-page []
  (fn []
    [:div.bg-gray-50
     [:main
      [:div {:class "mx-auto max-w-3xl px-4 sm:px-6 lg:max-w-7xl lg:px-8"}
       [:button {:on-click start} "Event"]]]]))

(defonce page (r/atom (home-page)))

(defn page-shell []
  (fn []
    @page
    [:div.bg-gray-50
     [:header {:class "bg-zinc-800 sticky top-0 z-10"}
      [:nav {:class "mx-auto flex max-w-7xl items-center justify-between p-4"}
       [:a {:href "/event" :class "-m-1.5 p-1.5 flex"}
         [:img {:class "h-16 w-auto" :src "https://raw.githubusercontent.com/nilenso/tarun-onboarding/main/swift-ticketing/swift-ticketing.png" :alt "swift-ticketing"}]
         [:span {:class "text-2xl font-bold text-amber-100 self-center px-4"} "Swift Ticketing"]]]]
     [:main
      [:div {:class "mx-auto max-w-3xl px-4 sm:px-6 lg:max-w-7xl lg:px-8"}
       [@page]]]]))

;; Routes
(secretary/defroute "/" []
  ; (d/render [home-page] (.getElementById js/document "app")))
  (reset! page (home-page)))

(secretary/defroute "/event" []
  (reset! page (event/events-page)))

(secretary/defroute "/event/create" []
  (reset! page (event/event-form)))

(secretary/defroute "/event/:event-id" [event-id]
  (reset! page (event/event-page event-id)))

(secretary/defroute "/event/:event-id/ticket/create" [event-id]
  (reset! page (ticket/ticket-form event-id)))

(secretary/defroute "/event/:event-id/ticket/:ticket-type-id" {:as params}
  (reset! page (ticket/seats-page (:event-id params) (:ticket-type-id params))))

(secretary/defroute "/booking/:booking-id" [booking-id]
  (reset! page (booking/booking-page booking-id)))

(secretary/defroute "/booking/payment/:booking-id" [booking-id]
  (reset! page (booking/payment-page booking-id)))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [page-shell] (.getElementById js/document "app")))

; (defn ^:export init! []
;   (mount-root))

(defn ^:export init! []
  (accountant/configure-navigation!
   {:nav-handler   (fn [path] (secretary/dispatch! path))
    :path-exists?  (fn [path] (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
