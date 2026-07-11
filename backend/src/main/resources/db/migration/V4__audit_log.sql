-- M-OS Stage 2: audit log infrastructure

CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100),
    description     TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT chk_audit_log_action CHECK (
        action IN (
            'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
            'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
            'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD'
        )
    )
);

CREATE INDEX idx_audit_log_game_session_id ON audit_log (game_session_id);
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
