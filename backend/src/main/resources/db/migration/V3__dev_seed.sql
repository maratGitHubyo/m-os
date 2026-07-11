-- M-OS Stage 1B: dev seed data (admin user + starting session)

INSERT INTO users (id, username, password_hash, nickname, active, created_at)
SELECT
    '10000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$hADQDps99NG7nGqMhNGdPeZQgS8PVEOO.O0uiQkU62.FDPV2Pkn3u',
    'Admin',
    TRUE,
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO game_sessions (id, name, date, status, created_at, updated_at)
SELECT
    '20000000-0000-0000-0000-000000000001',
    'M-OS Dev Session',
    CURRENT_DATE,
    'STARTING',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM game_sessions WHERE id = '20000000-0000-0000-0000-000000000001'
);

INSERT INTO game_config (
    id,
    game_session_id,
    starting_coins,
    max_trade_offers,
    numbers_total,
    fog_of_war_enabled,
    secrets_enabled,
    leaderboard_enabled,
    custom_settings
)
SELECT
    '30000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    1000,
    5,
    50,
    TRUE,
    TRUE,
    TRUE,
    '{}'::jsonb
WHERE NOT EXISTS (
    SELECT 1 FROM game_config WHERE game_session_id = '20000000-0000-0000-0000-000000000001'
);

INSERT INTO session_participants (id, user_id, game_session_id, role, nickname_snapshot, joined_at)
SELECT
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'ADMIN',
    'Admin',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM session_participants
    WHERE user_id = '10000000-0000-0000-0000-000000000001'
      AND game_session_id = '20000000-0000-0000-0000-000000000001'
);
