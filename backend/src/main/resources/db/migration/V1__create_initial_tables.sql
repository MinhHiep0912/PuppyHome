
-- V1: Create the initial Puppy’s Home database tables.

-- 1. Administrator accounts.
-- Store password hashes only; never store plaintext passwords.
CREATE TABLE admins (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_admins_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. Services and products shown in the catalogue.
CREATE TABLE services (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    short_description VARCHAR(500) NULL,
    description TEXT NULL,
    image_url VARCHAR(2048) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_services_slug UNIQUE (slug),
    CONSTRAINT chk_services_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    INDEX idx_services_status_order (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. Guides and informational articles.
CREATE TABLE guides (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NULL,
    body LONGTEXT NOT NULL,
    cover_image_url VARCHAR(2048) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_guides_slug UNIQUE (slug),
    CONSTRAINT chk_guides_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    INDEX idx_guides_status_order (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. Price tables.
CREATE TABLE price_tables (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description TEXT NULL,
    template_key VARCHAR(50) NOT NULL DEFAULT 'simple-list',
    layout_config JSON NULL,
    published_snapshot JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_price_tables_slug UNIQUE (slug),
    CONSTRAINT chk_price_tables_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    INDEX idx_price_tables_status_order (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. Individual rows/items inside a price table.
CREATE TABLE price_table_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    price_table_id BIGINT UNSIGNED NOT NULL,
    service_id BIGINT UNSIGNED NULL,
    item_name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    price_amount DECIMAL(12,2) NULL,
    price_label VARCHAR(100) NULL,
    unit_label VARCHAR(50) NULL,
    note TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT chk_price_items_amount
        CHECK (price_amount IS NULL OR price_amount >= 0),
    CONSTRAINT fk_price_items_table
        FOREIGN KEY (price_table_id)
        REFERENCES price_tables (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_price_items_service
        FOREIGN KEY (service_id)
        REFERENCES services (id)
        ON DELETE SET NULL,
    INDEX idx_price_items_table_order (price_table_id, sort_order),
    INDEX idx_price_items_service (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 6. General notes attached to a price table.
CREATE TABLE price_table_notes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    price_table_id BIGINT UNSIGNED NOT NULL,
    note_text TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_price_notes_table
        FOREIGN KEY (price_table_id)
        REFERENCES price_tables (id)
        ON DELETE CASCADE,
    INDEX idx_price_notes_table_order (price_table_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
