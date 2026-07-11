-- M-OS Stage 12: quest system

CREATE TABLE quests (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL,
    target_config   JSONB NOT NULL DEFAULT '{}'::jsonb,
    reward_config   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quests_type CHECK (
        type IN ('COLLECT_ITEMS', 'FIND_LOCATIONS', 'COLLECT_NUMBERS', 'REACH_SCORE', 'CUSTOM')
    ),
    CONSTRAINT chk_quests_status CHECK (
        status IN ('ACTIVE', 'DISABLED')
    ),
    CONSTRAINT fk_quests_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE player_quests (
    id              UUID PRIMARY KEY,
    quest_id        UUID NOT NULL,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    status          VARCHAR(50) NOT NULL,
    progress        JSONB,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_quests_quest_user UNIQUE (quest_id, user_id),
    CONSTRAINT chk_player_quests_status CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT fk_player_quests_quest
        FOREIGN KEY (quest_id) REFERENCES quests (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_quests_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_quests_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_quests_game_session_id ON quests (game_session_id);
CREATE INDEX idx_quests_type ON quests (type);
CREATE INDEX idx_quests_status ON quests (status);
CREATE INDEX idx_player_quests_user_id ON player_quests (user_id);
CREATE INDEX idx_player_quests_game_session_id ON player_quests (game_session_id);
CREATE INDEX idx_player_quests_status ON player_quests (status);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED', 'NUMBER_GRANT',
        'QUEST_CREATE', 'QUEST_START', 'QUEST_PROGRESS'
    )
);
