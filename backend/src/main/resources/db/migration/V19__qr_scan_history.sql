-- M-OS Stage 19: QR public identifiers and scan history enhancements

ALTER TABLE qr_codes ADD COLUMN public_id UUID;
UPDATE qr_codes SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE qr_codes ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE qr_codes ADD CONSTRAINT uq_qr_codes_public_id UNIQUE (public_id);
CREATE INDEX idx_qr_codes_public_id ON qr_codes (public_id);

ALTER TABLE qr_codes ADD COLUMN title VARCHAR(255);
UPDATE qr_codes SET title = code WHERE title IS NULL;

ALTER TABLE qr_scans ADD COLUMN reward_given BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_qr_scans_user_id ON qr_scans (user_id);
