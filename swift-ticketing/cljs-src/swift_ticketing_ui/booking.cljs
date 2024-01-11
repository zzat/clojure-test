(ns swift-ticketing-ui.booking
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r]
   [accountant.core :as accountant]
   [swift-ticketing-ui.client :as client]
   [camel-snake-kebab.extras :as cske]
   [camel-snake-kebab.core :as csk]))

(defn booking-page [booking-id]
  (let [loading (r/atom true)
        booking-status (r/atom nil)
        handler (fn [[ok response]]
                  (if ok
                    (do
                      (reset! loading false)
                      (reset! booking-status
                              (:booking-status (cske/transform-keys csk/->kebab-case-keyword response))))
                    (do (js/console.log "Response else: ", response)
                        (reset! loading false))))]
    (fn []
      @booking-status
      (when (or (= "InProcess" @booking-status)
                  (= "PaymentPending" @booking-status)
                  (nil? @booking-status))
          (js/setTimeout
           (fn []
             (client/http-get
              (str "/booking/" booking-id "/status")
              handler))
           500)))))

        [:div {:class "flex min-h-full flex-1 flex-col justify-center py-12 sm:px-6 lg:px-8"}
         [:div {:class "mt-10 sm:mx-auto sm:w-full sm:max-w-[500px]"}
          [:div {:class "bg-white px-6 py-12 shadow sm:rounded-lg sm:px-12"}
           [:div {:class "space-y-6 text-slate-500"} (str "Booking #: " booking-id)]
           [:div {:class "space-y-6"}
            [:span "Status: "]
            (if (nil? @booking-status)
              [:span {:class "animate-pulse rounded bg-yellow-200 border-yellow-600 inline-block px-2 py-1 text-yellow-900"} "Processing..."]
              [:span {:class "rounded bg-sky-200 border-sky-600 inline-block px-2 py-1 text-sky-900"} @booking-status])
            (cond
              (= "Confirmed" @booking-status)
              [:img {:class "w-20 -rotate-45 float-right"
                     :src "https://cdn.pixabay.com/photo/2020/06/03/10/24/confirmed-5254376_1280.png"}]
              (= "Rejected" @booking-status)
              [:img {:class "w-20 -rotate-45 float-right"
                     :src "https://live.staticflickr.com/8182/7985611823_2a8cba9408_z.jpg"}]
              :else nil)]]]]))))

(defn post-payment [url handler]
  (ajax/ajax-request
   {:uri url
    :method :post
    :handler handler
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))

(defn payment-page [booking-id]
  (let [tickets (r/atom nil)]
    (r/create-class
     {:display-name "payment-page"
      :component-did-mount
      (fn []
        (let [handler (fn [[ok response]]
                        (if (and ok (pos? (count response)))
                          (reset! tickets
                                  (cske/transform-keys csk/->kebab-case-keyword response))
                          (accountant/navigate! (str "/booking/" booking-id))))]
          (client/http-get (str "/booking/" booking-id "/ticket") handler)))
      :reagent-render
      (fn []
        (let [handler (fn [[ok response]]
                        (if ok
                          (accountant/navigate! (str "/booking/" booking-id))
                          (js/alert "Payment Failed")))
              button-base-class (str "rounded-md border border-transparent "
                                     "px-4 py-2 text-base font-medium text-white shadow-sm "
                                     "hover:bg-indigo-700 focus:outline-none focus:ring-2 "
                                     "focus:ring-indigo-500 focus:ring-offset-2")
              ok-button-class (str "bg-indigo-600 " button-base-class)
              cancel-button-class (str "bg-red-600 " button-base-class)]
          (cond
            (nil? @tickets) [:span "Loading..."]
            :else [:div {:class "flex min-h-full flex-1 flex-col justify-center py-12 sm:px-6 lg:px-8"}
                   [:div {:class "mt-10 sm:mx-auto sm:w-full sm:max-w-[480px]"}
                    [:div {:class "bg-white px-6 py-12 shadow sm:rounded-lg sm:px-12"}
                     [:div {:class "space-y-6 text-sm text-gray-400"} (str "Booking #" booking-id)]
                     [:div.py-4.grid.grid-cols-2
                      [:div "Tickets: "] [:div.text-right (count @tickets)]
                      [:div "Price: "] [:div.text-right (reduce + (map :ticket-price @tickets))]]
                     [:div {:class "mt-10 flex flex-row-reverse gap-2"}
                      [:button {:class ok-button-class
                                :on-click #(post-payment
                                            (str API_URL "/booking/" booking-id "/payment")
                                            handler)} "Make Payment"]
                      [:button {:class cancel-button-class
                                :on-click #(post-payment
                                            (str API_URL "/booking/" booking-id "/cancel")
                                            handler)} "Cancel Booking"]]]]])))})))

