-- M-OS Stage 6: QR codes and scan tracking

CREATE TABLE qr_codes (
    id                UUID PRIMARY KEY,
    game_session_id   UUID NOT NULL,
    location_point_id UUID,
    code              VARCHAR(255) NOT NULL,
    reward_type       VARCHAR(50) NOT NULL,
    reward_payload    JSONB NOT NULL DEFAULT '{}'::jsonb,
    scan_policy       VARCHAR(50) NOT NULL,
    scan_limit        INTEGER,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_qr_codes_session_code UNIQUE (game_session_id, code),
    CONSTRAINT chk_qr_codes_reward_type CHECK (
        reward_type IN ('COIN', 'ITEM', 'NONE')
    ),
    CONSTRAINT chk_qr_codes_scan_policy CHECK (
        scan_policy IN ('FIRST_PLAYER', 'EVERY_PLAYER', 'LIMITED')
    ),
    CONSTRAINT chk_qr_codes_scan_limit CHECK (
        scan_policy <> 'LIMITED' OR (scan_limit IS NOT NULL AND scan_limit > 0)
    ),
    CONSTRAINT fk_qr_codes_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_qr_codes_location_point
        FOREIGN KEY (location_point_id) REFERENCES location_points (id) ON DELETE SET NULL
);

CREATE TABLE qr_scans (
    id              UUID PRIMARY KEY,
    qr_code_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    game_session_id UUID NOT NULL,
    scanned_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_qr_scans_user_qr_code UNIQUE (user_id, qr_code_id),
    CONSTRAINT fk_qr_scans_qr_code
        FOREIGN KEY (qr_code_id) REFERENCES qr_codes (id) ON DELETE CASCADE,
    CONSTRAINT fk_qr_scans_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_qr_scans_game_session
        FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_qr_codes_game_session_id ON qr_codes (game_session_id);
CREATE INDEX idx_qr_codes_code ON qr_codes (code);
CREATE INDEX idx_qr_scans_qr_code_id ON qr_scans (qr_code_id);
CREATE INDEX idx_qr_scans_game_session_id ON qr_scans (game_session_id);
