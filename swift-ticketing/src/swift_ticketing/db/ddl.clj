(ns swift-ticketing.db.ddl
  (:require [honey.sql :as sql]))

(defn create-schema [schema-name]
  (str "CREATE SCHEMA " schema-name))

(defn delete-schema [schema-name]
  (str "DROP SCHEMA IF EXISTS " schema-name " CASCADE"))

(def create-type-booking-status
  (sql/format [:raw "CREATE TYPE booking_status AS ENUM ('InProcess','Canceled','Confirmed','PaymentPending','Rejected')"]))

(def create-type-ticket-status
  (sql/format [:raw "CREATE TYPE ticket_status AS ENUM ('Available','Booked','Reserved')"]))

(def create-type-seat-type
  (sql/format [:raw "CREATE TYPE seat_type AS ENUM ('General','Named')"]))

(def create-user-table
  (sql/format {:create-table :user_account
               :with-columns
               [[:user_id :uuid [:not nil]]
                [:name :text [:not nil]]
                [:created_at :timestamptz [:default [:now]]]
                [:updated_at :timestamptz [:default [:now]]]]
              }))

(def create-event-table
  (sql/format {:create-table :event
               :with-columns
               [[:event_id :uuid [:not nil]]
                [:event_name :text [:not nil]]
                [:event_description :text]
                [:event_date :timestamptz [:not nil]]
                [:venue :text]
                [:organizer_id :uuid [:not nil]]
                [:created_at :timestamptz [:default [:now]]]
                [:updated_at :timestamptz [:default [:now]]]]
              }))

(def create-ticket-type-table
  (sql/format {:create-table :ticket_type
               :with-columns
               [[:ticket_type_id :uuid [:not nil]]
                [:ticket_type :text [:not nil]]
                [:ticket_type_description :text]
                [:event_id :uuid [:not nil]]
                ; [:ticket_description :text]
                [:ticket_price :numeric [:not nil]]
                [:reservation_timelimit_seconds :int]
                [:seat_type :seat_type]
                [:created_at :timestamptz [:default [:now]]]
                [:updated_at :timestamptz [:default [:now]]]]
              }))

(def create-ticket-table
  (sql/format {:create-table :ticket
               :with-columns
               [[:ticket_id :uuid [:not nil]]
                [:ticket_name :text [:not nil]]
                [:ticket_type_id :uuid [:not nil]]
                ; [:ticket_description :text]
                [:ticket_price :numeric [:not nil]]
                [:reservation_expiration_time :timestamptz]
                [:ticket_status :ticket_status]
                [:booking_id :uuid]
                [:created_at :timestamptz [:default [:now]]]
                [:updated_at :timestamptz [:default [:now]]]]
              }))

(def create-booking-table
  (sql/format {:create-table :booking
               :with-columns
               [[:booking_id :uuid [:not nil]]
                [:user_id :uuid [:not nil]]
                [:booking_status :booking_status]
                [:created_at :timestamptz [:default [:now]]]
                [:updated_at :timestamptz [:default [:now]]]]
              }))
