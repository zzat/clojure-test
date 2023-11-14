(ns swift-ticketing-ui.event
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r]
   [secretary.core :as secretary]))

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
