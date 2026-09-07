CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    motorcycle VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE ride_room (
    id UUID PRIMARY KEY,
    room_code VARCHAR(6) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    max_members INTEGER NOT NULL CHECK (max_members BETWEEN 2 AND 50),
    public_room BOOLEAN NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE room_member (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES ride_room(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_room_member UNIQUE (room_id, user_id)
);

CREATE TABLE ride_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    distance_meters DOUBLE PRECISION NOT NULL DEFAULT 0,
    max_speed_kmh DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE ride_point (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES ride_session(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION NOT NULL DEFAULT 0,
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_room_member_user ON room_member(user_id);
CREATE INDEX idx_ride_session_user_started ON ride_session(user_id, started_at DESC);
CREATE INDEX idx_ride_point_ride_time ON ride_point(ride_id, recorded_at);
