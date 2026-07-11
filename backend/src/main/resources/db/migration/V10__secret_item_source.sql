-- Extend player_items acquired_from for secret rewards

ALTER TABLE player_items DROP CONSTRAINT chk_player_items_acquired_from;

ALTER TABLE player_items ADD CONSTRAINT chk_player_items_acquired_from CHECK (
    acquired_from IN ('QR', 'TRADE', 'QUEST', 'ADMIN', 'TRANSFER', 'SECRET')
);
