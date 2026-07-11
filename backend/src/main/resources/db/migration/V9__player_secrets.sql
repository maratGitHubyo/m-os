-- M-OS Stage 7: player secrets

CREATE TABLE player_secrets (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    code            VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    reward_type     VARCHAR(50) NOT NULL,
    reward_payload  JSONB NOT NULL DEFAULT '{}'::jsonb,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_secrets_session_code UNIQUE (game_session_id, code),
    CONSTRAINT chk_player_secrets_reward_type CHECK (
        reward_type IN ('COIN', 'ITEM', 'NUMBER', 'QUEST', 'NONE')
    ),
    CONSTRAINT fk_player_secrets_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_secrets_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_player_secrets_user_id ON player_secrets (user_id);
CREATE INDEX idx_player_secrets_game_session_id ON player_secrets (game_session_id);
CREATE INDEX idx_player_secrets_code ON player_secrets (code);
