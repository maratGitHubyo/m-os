-- M-OS Stage 13: player trades

CREATE TABLE trades (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    initiator_id    UUID NOT NULL,
    receiver_id     UUID NOT NULL,
    status          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_trades_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')
    ),
    CONSTRAINT chk_trades_different_participants CHECK (initiator_id <> receiver_id),
    CONSTRAINT fk_trades_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_trades_initiator
        FOREIGN KEY (initiator_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_trades_receiver
        FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE trade_items (
    id              UUID PRIMARY KEY,
    trade_id        UUID NOT NULL,
    player_item_id  UUID NOT NULL,
    owner_id        UUID NOT NULL,
    CONSTRAINT fk_trade_items_trade
        FOREIGN KEY (trade_id) REFERENCES trades (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_items_player_item
        FOREIGN KEY (player_item_id) REFERENCES player_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_items_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE trade_coins (
    id          UUID PRIMARY KEY,
    trade_id    UUID NOT NULL,
    user_id     UUID NOT NULL,
    amount      INTEGER NOT NULL,
    CONSTRAINT chk_trade_coins_amount CHECK (amount > 0),
    CONSTRAINT fk_trade_coins_trade
        FOREIGN KEY (trade_id) REFERENCES trades (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_coins_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_trades_game_session_id ON trades (game_session_id);
CREATE INDEX idx_trades_initiator_id ON trades (initiator_id);
CREATE INDEX idx_trades_receiver_id ON trades (receiver_id);
CREATE INDEX idx_trades_status ON trades (status);
CREATE INDEX idx_trade_items_trade_id ON trade_items (trade_id);
CREATE INDEX idx_trade_coins_trade_id ON trade_coins (trade_id);

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED', 'NUMBER_GRANT',
        'QUEST_CREATE', 'QUEST_START', 'QUEST_PROGRESS',
        'TRADE_CREATE', 'TRADE_ACCEPT', 'TRADE_DECLINE', 'TRADE_CANCEL'
    )
);
