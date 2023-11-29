CREATE SCHEMA swift_ticketing;

CREATE TABLE swift_ticketing.user_account (
  user_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- CREATE TABLE swift_ticketing.venue (
--   venue_id UUID PRIMARY KEY,
--   venue_name TEXT NOT NULL,
--   venue_description TEXT,
--   location POINT NOT NULL,  -- Add lat lng here
--   created_at timestamptz NOT NULL DEFAULT now(),
--   updated_at timestamptz NOT NULL DEFAULT now()
-- );
--
-- CREATE TABLE swift_ticketing.event (
--   event_id UUID PRIMARY KEY,
--   event_name TEXT NOT NULL,
--   event_description TEXT,
--   event_date_from timestamptz NOT NULL,
--   event_date_to timestamptz NOT NULL,
--   venue_id UUID NOT NULL,
--   organizer_id UUID NOT NULL,
--   created_at timestamptz NOT NULL DEFAULT now(),
--   updated_at timestamptz NOT NULL DEFAULT now(),
--   FOREIGN KEY(venue_id) REFERENCES swift_ticketing.venue(venue_id),
--   FOREIGN KEY(organizer_id) REFERENCES swift_ticketing.user(user_id)
-- );
CREATE TABLE swift_ticketing.event (
  event_id UUID PRIMARY KEY,
  event_name TEXT NOT NULL,
  event_description TEXT,
  event_date timestamptz NOT NULL,
  venue TEXT NOT NULL,
  organizer_id UUID NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(organizer_id) REFERENCES swift_ticketing.user_account(user_id)
);

-- CREATE TABLE swift_ticketing.category (
--   category_id UUID PRIMARY KEY,
--   category_name TEXT NOT NULL,
--   category_parent UUID,
--   created_at timestamptz NOT NULL DEFAULT now(),
--   updated_at timestamptz NOT NULL DEFAULT now(),
--   FOREIGN KEY(category_parent) REFERENCES swift_ticketing.category(category_id)
-- );

-- CREATE TABLE swift_ticketing.event_category (
--   category_id UUID PRIMARY KEY,
--   event_id UUID NOT NULL,
--   created_at timestamptz NOT NULL DEFAULT now(),
--   updated_at timestamptz NOT NULL DEFAULT now(),
--   FOREIGN KEY(category_id) REFERENCES swift_ticketing.category(category_id),
--   FOREIGN KEY(event_id) REFERENCES swift_ticketing.event(event_id)
-- );

CREATE TYPE swift_ticketing.booking_status AS ENUM ('Confirmed', 'Canceled', 'InProcess', 'PaymentPending', 'Rejected');
CREATE TYPE swift_ticketing.ticket_status AS ENUM ('Available', 'Booked', 'Reserved');
CREATE TYPE swift_ticketing.seat_type AS ENUM ('General', 'Named');

CREATE TABLE swift_ticketing.booking (
  booking_id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  booking_status swift_ticketing.booking_status NOT NULL,
  -- ticket_count INT NOT NULL,
  -- amount NUMERIC NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  -- FOREIGN KEY(ticket_id) REFERENCES swift_ticketing.ticket(ticket_id),
  FOREIGN KEY(user_id) REFERENCES swift_ticketing.user_account(user_id)
);

CREATE TABLE swift_ticketing.ticket_type (
  ticket_type_id UUID PRIMARY KEY,
  ticket_type TEXT NOT NULL,
  ticket_type_description TEXT,
  event_id UUID NOT NULL,
  reservation_timelimit_seconds INT,
  seat_type swift_ticketing.seat_type NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(event_id) REFERENCES swift_ticketing.event(event_id)
);

CREATE TABLE swift_ticketing.ticket (
  ticket_id UUID PRIMARY KEY,
  ticket_name TEXT NULL,
  ticket_type_id UUID NOT NULL,
  -- ticket_timing_from timestamptz NOT NULL,
  -- ticket_timing_to timestamptz NOT NULL,
  ticket_price NUMERIC Not NULL,
  reservation_expiration_time timestamptz,
  ticket_status swift_ticketing.ticket_status NOT NULL,
  -- ticket_total INT NOT NULL,
  booking_id UUID,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(ticket_type_id) REFERENCES swift_ticketing.ticket_type(ticket_type_id),
  FOREIGN KEY(booking_id) REFERENCES swift_ticketing.booking(booking_id)
);

