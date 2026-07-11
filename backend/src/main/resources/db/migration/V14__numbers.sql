-- M-OS Stage 11: collectible numbers

CREATE TABLE collectible_numbers (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    number_value    INTEGER NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_collectible_numbers_session_value UNIQUE (game_session_id, number_value),
    CONSTRAINT fk_collectible_numbers_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE player_numbers (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    number_id       UUID NOT NULL,
    acquired_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_numbers_user_number UNIQUE (user_id, number_id),
    CONSTRAINT fk_player_numbers_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_numbers_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_numbers_number
        FOREIGN KEY (number_id) REFERENCES collectible_numbers (id) ON DELETE CASCADE
);

CREATE INDEX idx_collectible_numbers_game_session_id ON collectible_numbers (game_session_id);
CREATE INDEX idx_player_numbers_user_id ON player_numbers (user_id);
CREATE INDEX idx_player_numbers_game_session_id ON player_numbers (game_session_id);
CREATE INDEX idx_player_numbers_number_id ON player_numbers (number_id);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED', 'NUMBER_GRANT'
    )
);
