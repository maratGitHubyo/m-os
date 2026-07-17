-- Lore fragments: template flag, reveal toggle, session-wide promo codes

ALTER TABLE item_templates
    ADD COLUMN is_lore BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE game_config
    ADD COLUMN lore_revealed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE player_secrets
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE player_secrets
    ADD COLUMN is_shared BOOLEAN NOT NULL DEFAULT FALSE;

-- Shared secrets must not reference a specific owner
ALTER TABLE player_secrets DROP CONSTRAINT IF EXISTS fk_player_secrets_user;
ALTER TABLE player_secrets
    ADD CONSTRAINT fk_player_secrets_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE player_secrets
    ADD CONSTRAINT chk_player_secrets_shared_owner CHECK (
        (is_shared = TRUE AND user_id IS NULL)
        OR (is_shared = FALSE AND user_id IS NOT NULL)
    );

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;
ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'COIN_TRANSFER', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'NUMBER_GRANT',
        'QUEST_CREATE', 'QUEST_START', 'QUEST_PROGRESS', 'QUEST_CLOSE',
        'TRADE_CREATE', 'TRADE_ACCEPT', 'TRADE_DECLINE', 'TRADE_CANCEL',
        'SESSION_START', 'SESSION_PAUSE', 'SESSION_FINISH',
        'AUCTION_MODE_CHANGE', 'AUCTION_LOT_CREATE', 'AUCTION_LOT_OPEN',
        'AUCTION_LOT_SELL', 'AUCTION_LOT_CANCEL', 'AUCTION_BID',
        'LORE_REVEAL_CHANGE'
    )
);
