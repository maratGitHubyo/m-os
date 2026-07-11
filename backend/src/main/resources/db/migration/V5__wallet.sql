-- M-OS Stage 3: wallet and coin transactions

CREATE TABLE wallets (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    balance         BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallets_user_session UNIQUE (user_id, game_session_id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wallets_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE coin_transactions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    amount          BIGINT NOT NULL,
    type            VARCHAR(50) NOT NULL,
    reference_id    VARCHAR(100),
    description     TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_coin_transactions_type CHECK (
        type IN ('REWARD', 'TRADE', 'ADMIN', 'QR', 'QUEST', 'SECRET', 'AUCTION')
    ),
    CONSTRAINT fk_coin_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_coin_transactions_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_game_session_id ON wallets (game_session_id);
CREATE INDEX idx_coin_transactions_user_session ON coin_transactions (user_id, game_session_id);
CREATE INDEX idx_coin_transactions_created_at ON coin_transactions (created_at DESC);
