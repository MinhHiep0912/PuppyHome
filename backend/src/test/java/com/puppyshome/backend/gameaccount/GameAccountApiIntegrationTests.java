package com.puppyshome.backend.gameaccount;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameAccountApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void gameAccountsTableMatchesContractSchema() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'game_accounts'
                          AND COLUMN_NAME IN (
                              'id', 'customer_id', 'name', 'nocape',
                              'status', 'created_at', 'updated_at')
                        """,
                Integer.class);
        assertEquals(7, columnCount);

        Integer gameIdColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'game_accounts'
                          AND COLUMN_NAME = 'game_id'
                        """,
                Integer.class);
        assertEquals(0, gameIdColumns);

        String fkTarget = jdbcTemplate.queryForObject(
                """
                        SELECT REFERENCED_TABLE_NAME
                        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'game_accounts'
                          AND COLUMN_NAME = 'customer_id'
                          AND REFERENCED_TABLE_NAME IS NOT NULL
                        """,
                String.class);
        assertEquals("customers", fkTarget);
    }

    @Test
    @Transactional
    void customerCreatesAccountWithDefaultsAndCannotSetOwnerOrStatus()
            throws Exception {
        String email = insertCustomer("create-" + randomSuffix());

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tài khoản test",
                                    "customerId": 999999,
                                    "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tài khoản test"))
                .andExpect(jsonPath("$.nocape").value(false))
                .andExpect(jsonPath("$.gameId").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.customerId").isNumber());

        Long customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM customers WHERE email = ?",
                Long.class,
                email);

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Có nocape",
                                    "nocape": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nocape").value(true));

        Integer ownedCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM game_accounts
                        WHERE customer_id = ? AND status = 'ACTIVE'
                        """,
                Integer.class,
                customerId);
        assertEquals(2, ownedCount);

        Integer foreignCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM game_accounts ga
                        JOIN customers c ON c.id = ga.customer_id
                        WHERE c.email = ? AND ga.customer_id <> ?
                        """,
                Integer.class,
                email,
                customerId);
        assertEquals(0, foreignCount);
    }

    @Test
    @Transactional
    void customerCannotAccessAnotherCustomersAccount() throws Exception {
        String ownerEmail = insertCustomer("owner-" + randomSuffix());
        String otherEmail = insertCustomer("other-" + randomSuffix());

        String location = mockMvc.perform(post("/api/game-accounts")
                        .with(customer(ownerEmail))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Account của owner\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        assertNotNull(location);
        long accountId = Long.parseLong(location.replace("/api/game-accounts/", ""));

        mockMvc.perform(get("/api/game-accounts/{id}", accountId)
                        .with(customer(otherEmail)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/game-accounts/{id}", accountId)
                        .with(customer(otherEmail))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Chiếm đoạt\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/game-accounts/{id}", accountId)
                        .with(customer(otherEmail))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/game-accounts/{id}/status", accountId)
                        .with(customer(otherEmail))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARCHIVED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/game-accounts")
                        .with(customer(otherEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Transactional
    void adminReadsAllAccountsButCannotWrite() throws Exception {
        String ownerEmail = insertCustomer("admin-read-" + randomSuffix());

        String location = mockMvc.perform(post("/api/game-accounts")
                        .with(customer(ownerEmail))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Admin đọc được\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        assertNotNull(location);
        long accountId = Long.parseLong(location.replace("/api/game-accounts/", ""));

        mockMvc.perform(get("/api/game-accounts").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem((int) accountId)));

        mockMvc.perform(get("/api/game-accounts/{id}", accountId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId));

        mockMvc.perform(post("/api/game-accounts")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Admin không được tạo\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/game-accounts/{id}", accountId)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Admin sửa\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/game-accounts/{id}/status", accountId)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARCHIVED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/game-accounts/{id}", accountId)
                        .with(admin())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/game-accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/game-accounts/{id}", 1L))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/game-accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Ẩn danh\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void mutationsRequireCsrfToken() throws Exception {
        String email = insertCustomer("csrf-" + randomSuffix());

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Thiếu CSRF\"}"))
                .andExpect(status().isForbidden());

        String location = mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Có CSRF\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        assertNotNull(location);
        long accountId = Long.parseLong(location.replace("/api/game-accounts/", ""));

        mockMvc.perform(put("/api/game-accounts/{id}", accountId)
                        .with(customer(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sửa không CSRF\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/game-accounts/{id}/status", accountId)
                        .with(customer(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARCHIVED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/game-accounts/{id}", accountId)
                        .with(customer(email)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/game-accounts/{id}", accountId)
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sửa có CSRF\", \"nocape\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sửa có CSRF"))
                .andExpect(jsonPath("$.nocape").value(true));
    }

    @Test
    @Transactional
    void validationBoundariesRejectInvalidInput() throws Exception {
        String email = insertCustomer("valid-" + randomSuffix());

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "a".repeat(129) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Ok\", \"nocape\": \"not-a-boolean\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "b".repeat(128) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void deleteArchivesKeepsRowAndPatchRestores() throws Exception {
        String email = insertCustomer("lifecycle-" + randomSuffix());

        String location = mockMvc.perform(post("/api/game-accounts")
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Vòng đời\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        assertNotNull(location);
        long accountId = Long.parseLong(location.replace("/api/game-accounts/", ""));

        mockMvc.perform(delete("/api/game-accounts/{id}", accountId)
                        .with(customer(email))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(
                "ARCHIVED",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM game_accounts WHERE id = ?",
                        String.class,
                        accountId));

        mockMvc.perform(delete("/api/game-accounts/{id}", accountId)
                        .with(customer(email))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/game-accounts/{id}", accountId)
                        .with(customer(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(patch("/api/game-accounts/{id}/status", accountId)
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/game-accounts/{id}/status", accountId)
                        .with(customer(email))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/game-accounts")
                        .with(customer(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    private RequestPostProcessor customer(String email) {
        return user(email).roles("CUSTOMER");
    }

    private RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private String insertCustomer(String suffix) {
        String email = suffix + "@example.com";
        jdbcTemplate.update(
                """
                        INSERT INTO customers (email, password_hash, display_name, status)
                        VALUES (?, 'not-a-real-hash', ?, 'ACTIVE')
                        """,
                email,
                "Customer " + suffix);
        return email;
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
