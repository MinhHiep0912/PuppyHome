-- V8: Unified user accounts — users + roles + user_roles + customer_profiles
--     + employee_profiles. Migrates data from admins/customers/employees.
-- Quyết định MT19a (2026-11-28): gộp mọi tài khoản về `users`, role qua
-- `user_roles`, profile theo role. `display_name` gom về `users` (deviation
-- có chủ đích so với E01: customer_profiles/employee_profiles không lặp lại
-- display_name).

CREATE TABLE roles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    description VARCHAR(255) NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (code),
    CONSTRAINT chk_roles_code
        CHECK (code IN ('CUSTOMER', 'EMPLOYEE', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, description) VALUES
    ('CUSTOMER', 'Khách hàng'),
    ('EMPLOYEE', 'Nhân viên'),
    ('ADMIN', 'Quản trị viên');

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    login_identifier VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT uq_users_login_identifier UNIQUE (login_identifier),
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_profiles (
    user_id BIGINT UNSIGNED NOT NULL,
    contact_phone VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    CONSTRAINT fk_customer_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_profiles (
    user_id BIGINT UNSIGNED NOT NULL,
    employee_code VARCHAR(64) NOT NULL,
    work_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    skill_group VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    CONSTRAINT uq_employee_profiles_code UNIQUE (employee_code),
    CONSTRAINT chk_employee_profiles_work_status
        CHECK (work_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_employee_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data migration: admins trước (id gốc), customers/empployees offset để tránh
-- trùng users.id. login_identifier: admin.username / customer.email /
-- employee.employee_code.
SET @customer_offset = (SELECT COUNT(*) FROM admins);
SET @employee_offset = @customer_offset + (SELECT COUNT(*) FROM customers);

INSERT INTO users (id, login_identifier, password_hash, display_name, status, created_at, updated_at)
SELECT id, username, password_hash, display_name,
       IF(is_active, 'ACTIVE', 'DISABLED'), created_at, updated_at
FROM admins;

INSERT INTO users (id, login_identifier, password_hash, display_name, status, created_at, updated_at)
SELECT id + @customer_offset, email, password_hash, display_name, status, created_at, updated_at
FROM customers;

-- Employee cũ chỉ là directory (chưa có credential): tạo users DISABLED với
-- password_hash sentinel, admin đặt lại mật khẩu qua PUT /api/admin/employees.
INSERT INTO users (id, login_identifier, password_hash, display_name, status, created_at, updated_at)
SELECT id + @employee_offset, employee_code, '!NO_PASSWORD_SET!', display_name,
       IF(work_status = 'ACTIVE', 'ACTIVE', 'DISABLED'), created_at, updated_at
FROM employees;

INSERT INTO user_roles (user_id, role_id)
SELECT a.id, r.id FROM admins a JOIN roles r ON r.code = 'ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT c.id + @customer_offset, r.id FROM customers c JOIN roles r ON r.code = 'CUSTOMER';

INSERT INTO user_roles (user_id, role_id)
SELECT e.id + @employee_offset, r.id FROM employees e JOIN roles r ON r.code = 'EMPLOYEE';

INSERT INTO customer_profiles (user_id, created_at, updated_at)
SELECT id + @customer_offset, created_at, updated_at FROM customers;

INSERT INTO employee_profiles (user_id, employee_code, work_status, created_at, updated_at)
SELECT id + @employee_offset, employee_code, work_status, created_at, updated_at FROM employees;

-- Repoint FK customer_id: customers.id -> users.id (id + @customer_offset).
ALTER TABLE game_accounts DROP FOREIGN KEY fk_game_accounts_customer;
UPDATE game_accounts ga
JOIN customers c ON ga.customer_id = c.id
SET ga.customer_id = c.id + @customer_offset;
ALTER TABLE game_accounts
    ADD CONSTRAINT fk_game_accounts_customer
        FOREIGN KEY (customer_id)
        REFERENCES users (id)
        ON DELETE RESTRICT;

ALTER TABLE service_requests DROP FOREIGN KEY fk_service_requests_customer;
UPDATE service_requests sr
JOIN customers c ON sr.customer_id = c.id
SET sr.customer_id = c.id + @customer_offset;
ALTER TABLE service_requests
    ADD CONSTRAINT fk_service_requests_customer
        FOREIGN KEY (customer_id)
        REFERENCES users (id)
        ON DELETE RESTRICT;

-- Repoint used_by_employee_id: employees.id -> employee_profiles.user_id.
ALTER TABLE invitation_codes DROP FOREIGN KEY fk_invitation_codes_employee;
UPDATE invitation_codes ic
JOIN employees e ON ic.used_by_employee_id = e.id
SET ic.used_by_employee_id = e.id + @employee_offset;
ALTER TABLE invitation_codes
    ADD CONSTRAINT fk_invitation_codes_employee
        FOREIGN KEY (used_by_employee_id)
        REFERENCES employee_profiles (user_id)
        ON DELETE RESTRICT;

SET @max_user_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM users);
SET @auto_increment_sql = CONCAT('ALTER TABLE users AUTO_INCREMENT = ', @max_user_id);
PREPARE auto_increment_stmt FROM @auto_increment_sql;
EXECUTE auto_increment_stmt;
DEALLOCATE PREPARE auto_increment_stmt;

DROP TABLE admins;
DROP TABLE customers;
DROP TABLE employees;
