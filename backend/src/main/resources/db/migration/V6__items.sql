-- M-OS Stage 4: items and inventory

CREATE TABLE item_templates (
    id              UUID PRIMARY KEY,
    game_session_id UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL,
    image_url       VARCHAR(1024),
    rarity          VARCHAR(50) NOT NULL,
    is_unique       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_item_templates_rarity CHECK (
        rarity IN ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')
    ),
    CONSTRAINT fk_item_templates_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE player_items (
    id               UUID PRIMARY KEY,
    item_template_id UUID NOT NULL,
    owner_id         UUID NOT NULL,
    game_session_id  UUID NOT NULL,
    acquired_from    VARCHAR(50) NOT NULL,
    acquired_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_player_items_acquired_from CHECK (
        acquired_from IN ('QR', 'TRADE', 'QUEST', 'ADMIN', 'TRANSFER')
    ),
    CONSTRAINT fk_player_items_template
        FOREIGN KEY (item_template_id) REFERENCES item_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_items_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_items_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE TABLE item_ownership_history (
    id              UUID PRIMARY KEY,
    player_item_id  UUID NOT NULL,
    game_session_id UUID NOT NULL,
    from_user_id    UUID,
    to_user_id      UUID NOT NULL,
    reason          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_item_ownership_history_reason CHECK (
        reason IN ('GRANT', 'TRANSFER', 'TRADE', 'ADMIN')
    ),
    CONSTRAINT fk_item_ownership_history_player_item
        FOREIGN KEY (player_item_id) REFERENCES player_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_ownership_history_from_user
        FOREIGN KEY (from_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_item_ownership_history_to_user
        FOREIGN KEY (to_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_ownership_history_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_item_templates_game_session_id ON item_templates (game_session_id);
CREATE INDEX idx_player_items_owner_id ON player_items (owner_id);
CREATE INDEX idx_player_items_game_session_id ON player_items (game_session_id);
CREATE INDEX idx_player_items_template_id ON player_items (item_template_id);
CREATE INDEX idx_item_ownership_history_player_item_id ON item_ownership_history (player_item_id);
