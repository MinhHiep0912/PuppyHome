-- V3: Create game accounts owned by customers.

CREATE TABLE game_accounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    nocape BOOLEAN NOT NULL DEFAULT FALSE,
    game_id VARCHAR(128) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_game_accounts_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_game_accounts_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    INDEX idx_game_accounts_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
