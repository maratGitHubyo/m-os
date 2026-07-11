-- M-OS Stage 1A: users and sessions domain

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    nickname        VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    avatar_url      VARCHAR(1024),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE game_sessions (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    date            DATE NOT NULL,
    status          VARCHAR(50) NOT NULL,
    map_image_url   VARCHAR(1024),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_game_sessions_status CHECK (
        status IN ('DRAFT', 'STARTING', 'ACTIVE', 'PAUSED', 'FINISHED')
    )
);

CREATE TABLE game_config (
    id                      UUID PRIMARY KEY,
    game_session_id         UUID NOT NULL,
    starting_coins          INTEGER NOT NULL DEFAULT 0,
    max_trade_offers        INTEGER NOT NULL DEFAULT 5,
    numbers_total           INTEGER NOT NULL DEFAULT 50,
    fog_of_war_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    secrets_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    leaderboard_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    custom_settings         JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_game_config_game_session_id UNIQUE (game_session_id),
    CONSTRAINT fk_game_config_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE session_participants (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    game_session_id     UUID NOT NULL,
    role                VARCHAR(50) NOT NULL,
    nickname_snapshot   VARCHAR(255) NOT NULL,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_session_participants_user_session UNIQUE (user_id, game_session_id),
    CONSTRAINT chk_session_participants_role CHECK (role IN ('PLAYER', 'ADMIN')),
    CONSTRAINT fk_session_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_session_participants_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_game_sessions_status ON game_sessions (status);
CREATE INDEX idx_session_participants_user_id ON session_participants (user_id);
CREATE INDEX idx_session_participants_game_session_id ON session_participants (game_session_id);
