-- Remove legacy demo users (alice, bob) and rename admin → marat

-- Drop Java-seeded duplicate host if present (keeps fixed UUID from V3)
DELETE FROM users WHERE username = 'marat';

DELETE FROM users WHERE username IN ('alice', 'bob');

UPDATE users
SET username = 'marat',
    nickname = 'Марат',
    password_hash = '$2a$10$EXcu/UIUKBQj7efok.4FWO6qYL7d/ImcExOyeQz/1dw17q8WCFtZW'
WHERE username = 'admin';

UPDATE session_participants
SET nickname_snapshot = 'Марат'
WHERE user_id = '10000000-0000-0000-0000-000000000001';
