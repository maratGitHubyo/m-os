-- Auction mode flag + lots/bids

ALTER TABLE game_config
    ADD COLUMN auction_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE auction_lots (
    id                   UUID PRIMARY KEY,
    game_session_id      UUID NOT NULL,
    title                VARCHAR(255) NOT NULL,
    starting_price       BIGINT NOT NULL DEFAULT 50,
    min_bid_increment    BIGINT NOT NULL DEFAULT 50,
    status               VARCHAR(50) NOT NULL,
    current_price        BIGINT,
    current_leader_id    UUID,
    winner_user_id       UUID,
    final_price          BIGINT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    opened_at            TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    CONSTRAINT chk_auction_lots_status CHECK (status IN ('DRAFT', 'OPEN', 'SOLD', 'CANCELLED')),
    CONSTRAINT chk_auction_lots_starting_price CHECK (starting_price >= 0),
    CONSTRAINT chk_auction_lots_min_bid_increment CHECK (min_bid_increment > 0),
    CONSTRAINT fk_auction_lots_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_lots_current_leader
        FOREIGN KEY (current_leader_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_auction_lots_winner
        FOREIGN KEY (winner_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_auction_lots_one_open_per_session
    ON auction_lots (game_session_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_auction_lots_game_session_id ON auction_lots (game_session_id);
CREATE INDEX idx_auction_lots_status ON auction_lots (status);

CREATE TABLE auction_bids (
    id                   UUID PRIMARY KEY,
    lot_id               UUID NOT NULL,
    game_session_id      UUID NOT NULL,
    bidder_user_id       UUID NOT NULL,
    amount               BIGINT NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_auction_bids_amount CHECK (amount > 0),
    CONSTRAINT fk_auction_bids_lot
        FOREIGN KEY (lot_id) REFERENCES auction_lots (id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_bids_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_bids_bidder
        FOREIGN KEY (bidder_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_auction_bids_lot_id ON auction_bids (lot_id);
CREATE INDEX idx_auction_bids_game_session_id ON auction_bids (game_session_id);
CREATE INDEX idx_auction_bids_bidder_user_id ON auction_bids (bidder_user_id);

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
        'AUCTION_LOT_SELL', 'AUCTION_LOT_CANCEL', 'AUCTION_BID'
    )
);
