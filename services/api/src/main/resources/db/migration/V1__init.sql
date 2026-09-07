CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE team_room (
    id UUID PRIMARY KEY,
    room_code VARCHAR(6) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    leader_id UUID NOT NULL REFERENCES user_account(id),
    max_members INTEGER NOT NULL CHECK (max_members BETWEEN 2 AND 100),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ
);

CREATE TABLE team_member (
    room_id UUID NOT NULL REFERENCES team_room(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX idx_team_member_user ON team_member(user_id);

CREATE TABLE latest_location (
    room_id UUID NOT NULL REFERENCES team_room(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    speed_mps DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    point GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
        ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
    ) STORED,
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX idx_latest_location_room ON latest_location(room_id);
CREATE INDEX idx_latest_location_point ON latest_location USING GIST(point);

CREATE TABLE ride_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account(id),
    room_id UUID REFERENCES team_room(id),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    distance_meters DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_speed_mps DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX idx_ride_session_user_started ON ride_session(user_id, started_at DESC);
CREATE INDEX idx_ride_session_status ON ride_session(status);

CREATE TABLE ride_track_point (
    ride_id UUID NOT NULL REFERENCES ride_session(id) ON DELETE CASCADE,
    sequence_no BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    speed_mps DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    point GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
        ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
    ) STORED,
    PRIMARY KEY (ride_id, sequence_no)
);
CREATE INDEX idx_ride_track_point_geo ON ride_track_point USING GIST(point);
