-- M-OS Stage 19: player coin transfers

CREATE TABLE coin_transfers (
    id               UUID PRIMARY KEY,
    game_session_id  UUID NOT NULL,
    sender_user_id   UUID NOT NULL,
    receiver_user_id UUID NOT NULL,
    amount           BIGINT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_coin_transfers_amount CHECK (amount > 0),
    CONSTRAINT chk_coin_transfers_different_users CHECK (sender_user_id <> receiver_user_id),
    CONSTRAINT fk_coin_transfers_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_coin_transfers_sender
        FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_coin_transfers_receiver
        FOREIGN KEY (receiver_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_coin_transfers_sender_user_id ON coin_transfers (sender_user_id);
CREATE INDEX idx_coin_transfers_receiver_user_id ON coin_transfers (receiver_user_id);
CREATE INDEX idx_coin_transfers_game_session_id ON coin_transfers (game_session_id);
CREATE INDEX idx_coin_transfers_created_at ON coin_transfers (created_at DESC);

ALTER TABLE coin_transactions DROP CONSTRAINT chk_coin_transactions_type;

ALTER TABLE coin_transactions ADD CONSTRAINT chk_coin_transactions_type CHECK (
    type IN (
        'REWARD', 'TRADE', 'ADMIN', 'QR', 'QUEST', 'SECRET', 'AUCTION',
        'TRANSFER_OUT', 'TRANSFER_IN'
    )
);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'COIN_TRANSFER', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED', 'NUMBER_GRANT',
        'QUEST_CREATE', 'QUEST_START', 'QUEST_PROGRESS',
        'TRADE_CREATE', 'TRADE_ACCEPT', 'TRADE_DECLINE', 'TRADE_CANCEL',
        'SESSION_START', 'SESSION_PAUSE', 'SESSION_FINISH'
    )
);
