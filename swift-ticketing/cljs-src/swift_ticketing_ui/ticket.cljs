(ns swift-ticketing-ui.ticket
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r]
   [secretary.core :as secretary]))

(defn post-ticket [url handler ticket-info]
  (let [{:keys [name description date venue]} ticket-info]
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

(defn ticket-form [event-id]
  (let [ticket-state (r/atom {:name ""
                             :description ""
                             :quantity 0
                             :price 0})
        handler (fn [[ok response]]
                  (if ok
                    (do
                      (secretary/dispatch! "/event"))
                    (js/alert "Couldn't create tickets!")))]
    (fn []
      [:div
       [:h2 "Enter Ticket Details"]
       [:form
        {:on-submit
         (fn [e]
           (post-ticket (str "http://127.0.0.1:9090/event/" event-id "/ticket") handler @ticket-state)
           (.preventDefault e))}
        [:label {:for "ticket-name"} "Ticket Name"]
        [:input
         {:type "text"
          :id "ticket-name"
          :value (:name @ticket-state)
          :on-change #(swap! ticket-state assoc :name (-> % .-target .-value))}]

        [:label {:for "ticket-description"} "Description"]
        [:input
         {:type "text"
          :id "ticket-description"
          :value (:description @ticket-state)
          :on-change #(swap! ticket-state assoc :description (-> % .-target .-value))}]

        [:label {:for "ticket-quantity"} "Quantity"]
        [:input
         {:type "number"
          :id "ticket-quantity"
          :value (:date @ticket-state)
          :on-change #(swap! ticket-state assoc :quantity (-> % .-target .-value))}]

        [:label {:for "ticket-price"} "Price"]
        [:input
         {:type "number"
          :id "ticket-price"
          :value (:price @ticket-state)
          :on-change #(swap! ticket-state assoc :price (-> % .-target .-value))}]

        [:button {:type "submit"} "Create Tickets"]]])))
