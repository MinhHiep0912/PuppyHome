-- V4: Drop game_id from game_accounts.
-- Dinh danh Game Account la PK `id`; invitation_codes se FK toi game_accounts(id).

ALTER TABLE game_accounts
    DROP COLUMN game_id;
