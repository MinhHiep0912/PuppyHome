package com.puppyshome.backend.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Helpers chèn dữ liệu auth thống nhất (users + user_roles + profiles)
 * cho integration tests. Tất cả phương thức idempotent theo login_identifier.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static long ensureUser(
            JdbcTemplate jdbc,
            String loginIdentifier,
            String roleCode) {
        return ensureUser(jdbc, loginIdentifier, roleCode, "test-hash", "ACTIVE");
    }

    public static long ensureUser(
            JdbcTemplate jdbc,
            String loginIdentifier,
            String roleCode,
            String passwordHash,
            String status) {
        Long existing = jdbc.query(
                "SELECT id FROM users WHERE login_identifier = ?",
                rs -> rs.next() ? rs.getLong(1) : null,
                loginIdentifier);
        long userId;
        if (existing == null) {
            jdbc.update(
                    """
                            INSERT INTO users (
                                login_identifier, password_hash,
                                display_name, status)
                            VALUES (?, ?, ?, ?)
                            """,
                    loginIdentifier,
                    passwordHash,
                    "User " + loginIdentifier,
                    status);
            userId = jdbc.queryForObject(
                    "SELECT id FROM users WHERE login_identifier = ?",
                    Long.class,
                    loginIdentifier);
        } else {
            userId = existing;
            jdbc.update(
                    "UPDATE users SET password_hash = ?, status = ? WHERE id = ?",
                    passwordHash,
                    status,
                    userId);
        }

        Long roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = ?",
                Long.class,
                roleCode);
        Integer linkCount = jdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM user_roles
                        WHERE user_id = ? AND role_id = ?
                        """,
                Integer.class,
                userId,
                roleId);
        if (linkCount == null || linkCount == 0) {
            jdbc.update(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                    userId,
                    roleId);
        }
        return userId;
    }

    public static long ensureCustomer(JdbcTemplate jdbc, String email) {
        long userId = ensureUser(jdbc, email, "CUSTOMER");
        Integer profileCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_profiles WHERE user_id = ?",
                Integer.class,
                userId);
        if (profileCount == null || profileCount == 0) {
            jdbc.update(
                    "INSERT INTO customer_profiles (user_id) VALUES (?)",
                    userId);
        }
        return userId;
    }

    public static long ensureEmployee(
            JdbcTemplate jdbc,
            String employeeCode,
            String displayName) {
        long userId = ensureUser(jdbc, employeeCode, "EMPLOYEE");
        jdbc.update(
                "UPDATE users SET display_name = ? WHERE id = ?",
                displayName,
                userId);
        Integer profileCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee_profiles WHERE user_id = ?",
                Integer.class,
                userId);
        if (profileCount == null || profileCount == 0) {
            jdbc.update(
                    """
                            INSERT INTO employee_profiles (
                                user_id, employee_code, work_status)
                            VALUES (?, ?, 'ACTIVE')
                            """,
                    userId,
                    employeeCode);
        }
        return userId;
    }
}
