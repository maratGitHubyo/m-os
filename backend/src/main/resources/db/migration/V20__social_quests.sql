-- M-OS: social honor quests (self-complete, completion limits)

ALTER TABLE quests DROP CONSTRAINT chk_quests_type;
ALTER TABLE quests ADD CONSTRAINT chk_quests_type CHECK (
    type IN ('COLLECT_ITEMS', 'FIND_LOCATIONS', 'COLLECT_NUMBERS', 'REACH_SCORE', 'CUSTOM', 'SOCIAL')
);

ALTER TABLE quests ADD COLUMN completion_policy VARCHAR(50) NOT NULL DEFAULT 'EVERY_PLAYER';
ALTER TABLE quests ADD COLUMN completion_limit INTEGER;

ALTER TABLE quests ADD CONSTRAINT chk_quests_completion_policy CHECK (
    completion_policy IN ('EVERY_PLAYER', 'LIMITED')
);
ALTER TABLE quests ADD CONSTRAINT chk_quests_completion_limit CHECK (
    completion_policy <> 'LIMITED' OR (completion_limit IS NOT NULL AND completion_limit > 0)
);

ALTER TABLE player_quests ADD COLUMN completion_note TEXT;

ALTER TABLE audit_log DROP CONSTRAINT chk_audit_log_action;
ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_action CHECK (
    action IN (
        'COIN_CREDIT', 'COIN_DEBIT', 'COIN_TRANSFER', 'ITEM_GRANT', 'ITEM_TRANSFER',
        'QR_SCAN', 'TRADE_COMPLETE', 'QUEST_COMPLETE', 'SECRET_REDEEM',
        'ADMIN_ACTION', 'LOCATION_DISCOVER', 'SCORE_AWARD', 'SCORE_ADD', 'SCORE_SUBTRACT',
        'EVENT_CREATE', 'EVENT_STATUS_CHANGE', 'VICTORY_ACHIEVED', 'NUMBER_GRANT',
        'QUEST_CREATE', 'QUEST_START', 'QUEST_PROGRESS', 'QUEST_CLOSE',
        'TRADE_CREATE', 'TRADE_ACCEPT', 'TRADE_DECLINE', 'TRADE_CANCEL',
        'SESSION_START', 'SESSION_PAUSE', 'SESSION_FINISH'
    )
);
