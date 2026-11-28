package com.puppyshome.backend.employee;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.puppyshome.backend.support.TestUsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void adminCreatesEmployeeAndEmployeeCanLogin() throws Exception {
        String code = "EMP-" + suffix();

        mockMvc.perform(post("/api/admin/employees")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "employeeCode": "%s",
                                    "displayName": "Nhân viên Test",
                                    "password": "TempPass123"
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.loginIdentifier").value(code))
                .andExpect(jsonPath("$.employeeCode").value(code))
                .andExpect(jsonPath("$.displayName").value("Nhân viên Test"))
                .andExpect(jsonPath("$.workStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(code, "TempPass123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(code))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_EMPLOYEE"));
    }

    @Test
    @Transactional
    void employeeEndpointsRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/employees"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/employees")
                        .with(user("someone").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/employees")
                        .with(user("someone").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"X\",\"displayName\":\"Y\",\"password\":\"12345678\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void inactiveWorkStatusDisablesLoginAndResetPasswordWorks()
            throws Exception {
        String code = "EMP-UPD-" + suffix();
        long userId = TestUsers.ensureEmployee(jdbcTemplate, code, "Old Name");
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ? WHERE id = ?",
                passwordEncoder.encode("TempPass123"),
                userId);

        mockMvc.perform(put("/api/admin/employees/{userId}", userId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "displayName": "Tên Mới",
                                    "workStatus": "INACTIVE",
                                    "password": "NewPass456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Tên Mới"))
                .andExpect(jsonPath("$.workStatus").value("INACTIVE"));

        assertEquals(
                "DISABLED",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM users WHERE id = ?",
                        String.class,
                        userId));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(code, "NewPass456")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/employees/{userId}", userId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workStatus\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(code, "NewPass456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_EMPLOYEE"));
    }

    @Test
    @Transactional
    void migratedDirectoryEmployeeCannotLoginUntilPasswordSet()
            throws Exception {
        String code = "EMP-LEGACY-" + suffix();
        jdbcTemplate.update(
                """
                        INSERT INTO users (
                            login_identifier, password_hash,
                            display_name, status)
                        VALUES (?, '!NO_PASSWORD_SET!', 'Legacy', 'DISABLED')
                        """,
                code);
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class,
                code);
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE code = 'EMPLOYEE'",
                Long.class);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                userId,
                roleId);
        jdbcTemplate.update(
                """
                        INSERT INTO employee_profiles (
                            user_id, employee_code, work_status)
                        VALUES (?, ?, 'ACTIVE')
                        """,
                userId,
                code);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(code, "anything123")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/employees/{userId}", userId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workStatus\":\"ACTIVE\",\"password\":\"ResetPass123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(code, "ResetPass123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_EMPLOYEE"));
    }

    private RequestPostProcessor admin() {
        TestUsers.ensureUser(jdbcTemplate, "admin", "ADMIN");
        return user("admin").roles("ADMIN");
    }

    private String loginJson(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
