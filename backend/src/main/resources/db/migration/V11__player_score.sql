-- M-OS Stage 8: player scores and score transactions

CREATE TABLE player_scores (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    category        VARCHAR(50) NOT NULL,
    points          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_player_scores_user_session_category
        UNIQUE (user_id, game_session_id, category),
    CONSTRAINT chk_player_scores_points CHECK (points >= 0),
    CONSTRAINT chk_player_scores_category CHECK (
        category IN ('TOTAL', 'EXPLORER', 'COLLECTOR', 'TRADER', 'QUEST')
    ),
    CONSTRAINT fk_player_scores_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_scores_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE score_transactions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    category        VARCHAR(50) NOT NULL,
    delta           BIGINT NOT NULL,
    reason          TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_score_transactions_category CHECK (
        category IN ('TOTAL', 'EXPLORER', 'COLLECTOR', 'TRADER', 'QUEST')
    ),
    CONSTRAINT fk_score_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_score_transactions_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_player_scores_game_session_id ON player_scores (game_session_id);
CREATE INDEX idx_player_scores_user_id ON player_scores (user_id);
CREATE INDEX idx_score_transactions_user_id ON score_transactions (user_id);
CREATE INDEX idx_score_transactions_game_session_id ON score_transactions (game_session_id);
CREATE INDEX idx_score_transactions_created_at ON score_transactions (created_at DESC);

-- Extend audit actions for score changes
ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT'
    )
);
