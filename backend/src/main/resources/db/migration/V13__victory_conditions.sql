-- M-OS Stage 10: victory conditions

CREATE TABLE victory_conditions (
    id                  UUID PRIMARY KEY,
    game_session_id     UUID NOT NULL,
    type                VARCHAR(50) NOT NULL,
    target_value        JSONB NOT NULL DEFAULT '{}'::jsonb,
    description         TEXT NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    achieved_at         TIMESTAMPTZ,
    achieved_by_user_id UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_victory_conditions_type CHECK (
        type IN (
            'COLLECT_ALL_NUMBERS',
            'COLLECT_UNIQUE_ITEMS',
            'REACH_SCORE',
            'FIND_ALL_LOCATIONS',
            'CUSTOM'
        )
    ),
    CONSTRAINT fk_victory_conditions_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_victory_conditions_achieved_by
        FOREIGN KEY (achieved_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_victory_conditions_game_session_id ON victory_conditions (game_session_id);
CREATE INDEX idx_victory_conditions_active ON victory_conditions (game_session_id, active);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED'
    )
);
