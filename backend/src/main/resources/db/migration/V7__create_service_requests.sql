ALTER TABLE services
    ADD COLUMN price_amount DECIMAL(12,0) NULL,
    ADD COLUMN price_currency CHAR(3) NULL DEFAULT 'VND',
    ADD COLUMN price_unit VARCHAR(32) NULL,
    ADD CONSTRAINT chk_services_price_amount
        CHECK (price_amount IS NULL OR price_amount >= 0);

CREATE TABLE service_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    game_account_id BIGINT UNSIGNED NOT NULL,
    service_id BIGINT UNSIGNED NOT NULL,
    snapshot_service_name VARCHAR(150) NOT NULL,
    snapshot_price_amount DECIMAL(12,0) NOT NULL,
    snapshot_currency CHAR(3) NOT NULL,
    snapshot_price_unit VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    status_actor_type VARCHAR(16) NULL,
    status_actor_id BIGINT UNSIGNED NULL,
    requested_start_at DATETIME(6) NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    duration_days INT NOT NULL DEFAULT 30,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_service_requests_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_game_account
        FOREIGN KEY (game_account_id)
        REFERENCES game_accounts (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_service
        FOREIGN KEY (service_id)
        REFERENCES services (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_service_requests_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT chk_service_requests_status_actor_type
        CHECK (status_actor_type IS NULL
            OR status_actor_type IN ('CUSTOMER', 'ADMIN', 'EMPLOYEE')),
    INDEX idx_service_requests_customer_status (customer_id, status),
    INDEX idx_service_requests_game_account (game_account_id),
    INDEX idx_service_requests_service (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
