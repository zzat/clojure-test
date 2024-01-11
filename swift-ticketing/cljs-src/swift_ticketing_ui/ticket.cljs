(ns swift-ticketing-ui.ticket
  (:require
   [reagent.core :as r]
   [swift-ticketing-ui.client :as client]
   [accountant.core :as accountant]
   [swift-ticketing-ui.theme :as theme]
   [camel-snake-kebab.extras :as cske]
   [camel-snake-kebab.core :as csk]))

(defn ticket-form [event-id]
  (let [ticket-state (r/atom {:name ""
                              :description ""
                              :quantity 0
                              :price 0})
        handler (fn [[ok _]]
                  (if ok
                    (accountant/navigate! "/event")
                    (js/alert "Couldn't create tickets!")))]
    (fn []
      [:div
       [:h2 "Enter Ticket Details"]
       [:form
        {:on-submit
         (fn [e]
           (client/http-post (str "/event/" event-id "/ticket")
                             handler
                             {:name (:name @ticket-state)
                              :description (:description @ticket-state)
                              :date (:date @ticket-state)
                              :venue (:venue @ticket-state)})
           (.preventDefault e))}
        [:label {:for "ticket-name"} "Ticket Name"]
        [:input
         {:type "text"
          :id "ticket-name"
          :value (:name @ticket-state)
          :on-change #(swap! ticket-state 
                             assoc :name (-> % .-target .-value))}]

        [:label {:for "ticket-description"} "Description"]
        [:input
         {:type "text"
          :id "ticket-description"
          :value (:description @ticket-state)
          :on-change #(swap! ticket-state 
                             assoc :description (-> % .-target .-value))}]

        [:label {:for "ticket-quantity"} "Quantity"]
        [:input
         {:type "number"
          :id "ticket-quantity"
          :value (:date @ticket-state)
          :on-change #(swap! ticket-state 
                             assoc :quantity (-> % .-target .-value))}]

        [:label {:for "ticket-price"} "Price"]
        [:input
         {:type "number"
          :id "ticket-price"
          :value (:price @ticket-state)
          :on-change #(swap! ticket-state 
                             assoc :price (-> % .-target .-value))}]

        [:button {:type "submit"} "Create Tickets"]]])))

(defn seats-layout [event-id seats]
  (let [handler (fn [[ok response]]
                  (if ok
                    (accountant/navigate! "/event")
                    (js/alert "Couldn't create tickets!")))
        booking-handler (fn [[ok response]]
                          (if ok
                            (do
                              (accountant/navigate! 
                                (str "/booking/payment/" 
                                     (:booking-id (cske/transform-keys csk/->kebab-case-keyword 
                                                                       response)))))
                            (js/alert "Booking Failed")))
        selected-seats-set (r/atom #{})]
    (fn [event-id seats]
      @selected-seats-set
      [:div {:class "mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:px-8 lg:max-w-7xl"}
       [:div {:class ""}
        [:div {:class "stage text-center rounded-t-[50%] border-t-8 border-lime-100 bg-gradient-to-b from-lime-50	to-transparent py-16 text-slate-400"} 
         "↑ Event this way ↑"]
        [:div {:class "seats grid grid-cols-8 gap-8"}
         (for [seat seats]
           (let [seat-bg (if (contains? @selected-seats-set (:ticket-id seat)) 
                           "bg-lime-500 border-lime-600 text-lime-800" 
                           "bg-sky-50 border-sky-100 text-cyan-700")
                 seat-class (str "py-4 px-2 text-center font-bold cursor-pointer transition-all duration-300 ease-in border-4 rounded-t-[50%] " 
                                 seat-bg)]
             ^{:key (:ticket-id seat)}
             [:div {:class seat-class
                    :on-click (fn [] (if (contains? @selected-seats-set (:ticket-id seat))
                                       (swap! selected-seats-set disj (:ticket-id seat))
                                       (swap! selected-seats-set conj (:ticket-id seat))))}
              (:ticket-name seat)]))]]
       [:div {:class "fixed bottom-0 left-0 w-full p-4 flex flex-row-reverse"}
        [:button {:class theme/primary-button-class
                  :disabled (empty? @selected-seats-set)
                  :on-click #(client/http-post
                              (str "/event/" event-id "/booking")
                              booking-handler
                              {:ticket_ids @selected-seats-set})} "Buy Tickets"]]])))

(defn seats-page [event-id ticket-type-id]
  (let [loading (r/atom true)
        seats (r/atom [])]
    (r/create-class
     {:display-name "seats-page"
      :component-did-mount
      (fn []
        (let [handler (fn [[ok response]]
                        (if ok
                          (do
                            (reset! loading false)
                            (reset! seats (cske/transform-keys csk/->kebab-case-keyword 
                                                               response)))
                          (reset! loading false)))]
          (client/http-get "/ticket" handler {:ticket_type_id ticket-type-id})))
      :reagent-render (fn []
                        (if @loading
                          [:div "Loading..."]
                          [seats-layout event-id @seats]))})))
