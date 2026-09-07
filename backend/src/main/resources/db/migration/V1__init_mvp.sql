CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  phone VARCHAR(32) UNIQUE,
  nickname VARCHAR(64) NOT NULL,
  avatar_url TEXT,
  motorcycle_model VARCHAR(128),
  invisible BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ride_team (
  id UUID PRIMARY KEY,
  room_code VARCHAR(12) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  owner_id UUID NOT NULL REFERENCES app_user(id),
  route_note VARCHAR(255),
  capacity INTEGER NOT NULL CHECK (capacity BETWEEN 2 AND 100),
  private_room BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  closed_at TIMESTAMPTZ
);

CREATE TABLE ride_team_member (
  team_id UUID NOT NULL REFERENCES ride_team(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
  muted BOOLEAN NOT NULL DEFAULT FALSE,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  left_at TIMESTAMPTZ,
  PRIMARY KEY (team_id, user_id)
);

CREATE TABLE ride_session (
  id UUID PRIMARY KEY,
  team_id UUID REFERENCES ride_team(id),
  user_id UUID NOT NULL REFERENCES app_user(id),
  started_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  distance_meters DOUBLE PRECISION NOT NULL DEFAULT 0,
  duration_seconds BIGINT NOT NULL DEFAULT 0,
  average_speed_kmh DOUBLE PRECISION NOT NULL DEFAULT 0,
  max_speed_kmh DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE ride_track_point (
  id BIGSERIAL PRIMARY KEY,
  ride_id UUID NOT NULL REFERENCES ride_session(id) ON DELETE CASCADE,
  sequence_no BIGINT NOT NULL,
  captured_at TIMESTAMPTZ NOT NULL,
  position GEOGRAPHY(POINT, 4326) NOT NULL,
  speed_kmh DOUBLE PRECISION,
  accuracy_meters DOUBLE PRECISION,
  UNIQUE (ride_id, sequence_no)
);

CREATE INDEX idx_track_point_position ON ride_track_point USING GIST(position);
CREATE INDEX idx_track_point_ride_time ON ride_track_point(ride_id, captured_at);
