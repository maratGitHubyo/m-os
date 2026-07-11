-- M-OS Stage 9: game events and participation

CREATE TABLE game_events (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    status          VARCHAR(50) NOT NULL,
    start_at        TIMESTAMPTZ,
    end_at          TIMESTAMPTZ,
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_game_events_type CHECK (
        type IN ('ANNOUNCEMENT', 'BONUS_PERIOD', 'LOCATION_REVEAL', 'LEADERBOARD_FREEZE', 'AUCTION', 'CUSTOM')
    ),
    CONSTRAINT chk_game_events_status CHECK (
        status IN ('SCHEDULED', 'RUNNING', 'PAUSED', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT fk_game_events_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_game_events_created_by
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE event_participations (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    event_id   UUID NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result     JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_event_participations_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_event_participations_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_participations_event
        FOREIGN KEY (event_id) REFERENCES game_events (id) ON DELETE CASCADE
);

CREATE INDEX idx_game_events_game_session_id ON game_events (game_session_id);
CREATE INDEX idx_game_events_status ON game_events (status);
CREATE INDEX idx_event_participations_event_id ON event_participations (event_id);
CREATE INDEX idx_event_participations_user_id ON event_participations (user_id);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE'
    )
);
