-- M-OS: assign quests to all players or a specific player

ALTER TABLE quests ADD COLUMN assignee_user_id UUID;

ALTER TABLE quests ADD CONSTRAINT fk_quests_assignee_user
    FOREIGN KEY (assignee_user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_quests_assignee_user_id ON quests (assignee_user_id);
