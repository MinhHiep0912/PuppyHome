-- V6: Create invitation codes for game accounts.

CREATE TABLE invitation_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    game_account_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(128) NOT NULL,
    note VARCHAR(255) NULL,
    used_by_employee_id BIGINT UNSIGNED NULL,
    used_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_invitation_codes_game_account
        FOREIGN KEY (game_account_id)
        REFERENCES game_accounts (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_codes_employee
        FOREIGN KEY (used_by_employee_id)
        REFERENCES employees (id)
        ON DELETE RESTRICT,
    INDEX idx_invitation_codes_game_account (game_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
