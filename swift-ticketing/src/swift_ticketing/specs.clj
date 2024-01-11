(ns swift-ticketing.specs
  (:require
   [malli.core :as m]
   [malli.registry :as mr]))

(defn date? [date]
  (let [date-regex #"\d{4}-\d{2}-\d{2}"]
    (boolean
     (and (string? date) (re-matches date-regex date)))))

(defn string-uuid? [x]
  (try
    (java.util.UUID/fromString x)
    (catch Exception _ false)))

(def non-empty-string
  (m/schema [:string {:min 1}]))

(mr/set-default-registry!
 (mr/composite-registry
  (m/default-schemas)
  {:date (m/-simple-schema {:type :date
                            :pred date?
                            :type-properties {:error/message
                                              "expected format yyyy-MM-dd"}})
   :non-empty-string non-empty-string
   :string-uuid (m/-simple-schema {:type :string-uuid
                                   :pred string-uuid?
                                   :type-properties {:error/message
                                                     "should be a uuid"}})}))

(def GetEventsParams
  [:map
   [:venue {:optional true} :non-empty-string]
   [:from {:optional true} :date]
   [:to {:optional true} :date]])

(def CreateEventParams
  [:map
   [:name :non-empty-string]
    [:description :non-empty-string]
    [:date :date]
    [:venue :non-empty-string]])

(def CreateTicketsParams
  (let [seat-schema
        [:map
         [:name :non-empty-string]]]
    [:multi {:dispatch :seat-type}
     ["General"
      [:map
       [:ticket-type :non-empty-string]
       [:seat-type :non-empty-string]
       [:description :string]
       [:quantity pos-int?]
       [:price pos?]]]
     ["Named"
      [:map
       [:ticket-type :non-empty-string]
       [:seat-type :non-empty-string]
       [:description :string]
       [:seats [:vector seat-schema]]
       [:reservation-limit-in-seconds nat-int?]
       [:price pos?]]]]))

(def ReserveTicketsParams
  [:or
   [:map
    [:ticket-ids [:vector :string-uuid]]]
   [:map
    [:ticket-type-id :string-uuid]
    [:quantity pos-int?]]])

(def EventId :string-uuid)
(def BookingId :string-uuid)
(def TicketTypeId :string-uuid)
