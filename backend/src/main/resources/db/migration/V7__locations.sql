-- M-OS Stage 5: location points and fog of war discoveries

CREATE TABLE location_points (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    x               DOUBLE PRECISION NOT NULL,
    y               DOUBLE PRECISION NOT NULL,
    zone            VARCHAR(100) NOT NULL,
    hidden          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_location_points_x CHECK (x >= 0 AND x <= 100),
    CONSTRAINT chk_location_points_y CHECK (y >= 0 AND y <= 100),
    CONSTRAINT fk_location_points_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE player_location_discoveries (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL,
    location_point_id UUID NOT NULL,
    game_session_id   UUID NOT NULL,
    discovered_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_location_discoveries_user_location
        UNIQUE (user_id, location_point_id),
    CONSTRAINT fk_player_location_discoveries_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_location_discoveries_location
        FOREIGN KEY (location_point_id) REFERENCES location_points (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_location_discoveries_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_location_points_game_session_id ON location_points (game_session_id);
CREATE INDEX idx_player_location_discoveries_user_id ON player_location_discoveries (user_id);
CREATE INDEX idx_player_location_discoveries_location_point_id ON player_location_discoveries (location_point_id);
CREATE INDEX idx_player_location_discoveries_game_session_id ON player_location_discoveries (game_session_id);
