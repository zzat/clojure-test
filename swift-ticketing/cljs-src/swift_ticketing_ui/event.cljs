(ns swift-ticketing-ui.event
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r]
   [secretary.core :as secretary]
   [camel-snake-kebab.extras :as cske]
   [camel-snake-kebab.core :as csk]))

(defn post-event [url handler event-info]
  (let [{:keys [name description date venue]} event-info]
    (ajax/ajax-request
     {:uri url
      :method :post
      :params {:name name
               :description description
               :date date
               :venue venue}
      :handler handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))

(defn event-form []
  (let [event-state (r/atom {:name ""
                             :description ""
                             :date ""
                             :venue ""})
        handler (fn [[ok response]]
                  (if ok
                    (do
                      (secretary/dispatch! "/event"))
                    (js/alert "Couldn't create the Event")))]
    (fn []
      [:div
       [:h2 "Enter Event Details"]
       [:form
        {:on-submit
         (fn [e]
           (post-event "http://127.0.0.1:9090/event" handler @event-state)
           (.preventDefault e))}
        [:label {:for "event-name"} "Event Name"]
        [:input
         {:type "text"
          :id "event-name"
          :value (:name @event-state)
          :on-change #(swap! event-state assoc :name (-> % .-target .-value))}]

        [:label {:for "event-description"} "Description"]
        [:input
         {:type "text"
          :id "event-description"
          :value (:description @event-state)
          :on-change #(swap! event-state assoc :description (-> % .-target .-value))}]

        [:label {:for "event-date"} "Event Date"]
        [:input
         {:type "date"
          :id "event-date"
          :value (:date @event-state)
          :on-change #(swap! event-state assoc :date (-> % .-target .-value))}]

        [:label {:for "event-location"} "Event Location"]
        [:input
         {:type "text"
          :id "event-location"
          :value (:venue @event-state)
          :on-change #(swap! event-state assoc :venue (-> % .-target .-value))}]

        [:button {:type "submit"} "Submit"]]])))

(defn get-events [url handler]
  (ajax/ajax-request
   {:uri url
    :method :get
    :handler handler
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))

(defn event-card [event]
  [:a {:href "#" :class "group" :on-click (fn [] (secretary/dispatch! (str "/event/" (:event-id event))))}
   [:div {:class "aspect-h-1 aspect-w-1 w-full overflow-hidden rounded-lg sm:aspect-h-3 sm:aspect-w-2"}
    [:img {:src "https://images.pexels.com/photos/976866/pexels-photo-976866.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2" :class "h-full w-full object-cover object-center group-hover:opacity-75" :alt "Product"}]]
   [:div {:class "mt-4 flex items-center justify-between text-base font-medium text-gray-900"}
    [:h3 (:event-name event)]
    [:p (:event-date event)]]
   [:p {:class "mt-1 text-sm italic text-gray-500"} (:event-description event)]])

(defn events-list [events]
  [:div#events-page
   [:div {:class "py-24 text-center"}
    [:h1 {:class "text-4xl font-bold tracking-tight text-gray-900"} "Events"]
    [:p {:class "mx-auto mt-4 max-w-3xl text-base text-gray-500"} "Book Events"]]
   [:section.mt-8
    [:h2#products-heading.sr-only "Events"]
    [:div {:class "grid grid-cols-1 gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3 xl:gap-x-8"}
     (for [event events]
       ^{:key (:id (:event_id event))} [event-card event])]]])

(defn events-page []
  (let [loading (r/atom true)
        events (r/atom [])]
    (r/create-class
     {:display-name "events-page"
      :component-did-mount
      (fn []
        (let [handler (fn [[ok response]]
                        (if ok
                          (do
                            (reset! loading false)
                            (reset! events
                                    (cske/transform-keys csk/->kebab-case-keyword response)))
                          (do (js/console.log "Response else: ", response)
                              (reset! loading false))))]
          (get-events "http://127.0.0.1:9090/event" handler)))
      :reagent-render (fn []
                        (if @loading
                          [:div "Loading..."]
                          [events-list @events]))})))
