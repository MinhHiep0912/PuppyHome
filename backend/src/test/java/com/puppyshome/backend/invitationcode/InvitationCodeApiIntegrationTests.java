package com.puppyshome.backend.invitationcode;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvitationCodeApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaMatchesInvitationCodeAndEmployeeContract() {
        Integer employeeColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'employees'
                          AND COLUMN_NAME IN (
                              'id', 'employee_code', 'display_name',
                              'work_status', 'created_at', 'updated_at')
                        """,
                Integer.class);
        assertEquals(6, employeeColumns);

        Integer forbiddenColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'invitation_codes'
                          AND COLUMN_NAME IN (
                              'internal_id', 'employee_id', 'expires_at', 'status')
                        """,
                Integer.class);
        assertEquals(0, forbiddenColumns);

        Integer codeColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'invitation_codes'
                          AND COLUMN_NAME IN (
                              'id', 'game_account_id', 'code', 'note',
                              'used_by_employee_id', 'used_at', 'deleted_at',
                              'created_at', 'updated_at')
                        """,
                Integer.class);
        assertEquals(9, codeColumns);
    }

    @Test
    @Transactional
    void customerOwnershipAndAdminAccess() throws Exception {
        String owner = insertCustomer("invite-owner-" + suffix());
        String other = insertCustomer("invite-other-" + suffix());
        long accountId = insertAccount(owner);

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABC123\",\"note\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usedByEmployeeId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.usedByEmployeeName").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.usedAt").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(other)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ADMIN-CODE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/game-accounts/{id}/invitation-codes", accountId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void archivedAccountRejectsPostButKeepsPutDelete() throws Exception {
        String email = insertCustomer("invite-archived-" + suffix());
        long accountId = insertAccount(email);
        long codeId = insertCode(accountId, "OLD-CODE");
        jdbcTemplate.update("UPDATE game_accounts SET status = 'ARCHIVED' WHERE id = ?", accountId);

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEW-CODE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("ACCOUNT_NOT_ACTIVE"));

        mockMvc.perform(put("/api/invitation-codes/{codeId}", codeId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"UPDATED-CODE\",\"note\":\"Da luu tru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED-CODE"));

        mockMvc.perform(delete("/api/invitation-codes/{codeId}", codeId)
                        .with(customer(email)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void deleteIsSoftAndIdempotentAndDeletedCodeIsHidden() throws Exception {
        String email = insertCustomer("invite-delete-" + suffix());
        long accountId = insertAccount(email);
        long codeId = insertCode(accountId, "DELETE-ME");

        mockMvc.perform(delete("/api/invitation-codes/{codeId}", codeId)
                        .with(customer(email)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/invitation-codes/{codeId}", codeId)
                        .with(customer(email)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put("/api/invitation-codes/{codeId}", codeId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NOPE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void validationAndCsrfAndAdminMutationRules() throws Exception {
        String email = insertCustomer("invite-validation-" + suffix());
        long accountId = insertAccount(email);

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NO-CSRF\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + "x".repeat(129) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/game-accounts/{id}/invitation-codes", accountId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ADMIN-WORKS\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/game-accounts")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden root mutation\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void markUsedIsOneWayAndReturnsEmployeeName() throws Exception {
        String email = insertCustomer("invite-used-" + suffix());
        long accountId = insertAccount(email);
        long codeId = insertCode(accountId, "USE-ME");
        long employeeId = insertEmployee("EMP-" + suffix(), "Nguyen Employee");

        long freshCodeId = insertCode(accountId, "EMPLOYEE-NOT-FOUND");
        mockMvc.perform(post("/api/invitation-codes/{codeId}/mark-used", freshCodeId)
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/invitation-codes/{codeId}/mark-used", freshCodeId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedByEmployeeId").value(employeeId))
                .andExpect(jsonPath("$.usedByEmployeeName").value("Nguyen Employee"))
                .andExpect(jsonPath("$.usedAt").exists());

        mockMvc.perform(post("/api/invitation-codes/{codeId}/mark-used", freshCodeId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("CODE_ALREADY_USED"));

        long missingEmployeeCodeId = insertCode(accountId, "MISSING-EMPLOYEE");
        mockMvc.perform(post("/api/invitation-codes/{codeId}/mark-used", missingEmployeeCodeId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":999999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void adminEmployeesListCreateAndDuplicateCode() throws Exception {
        String duplicateCode = "EMP-DUP-" + suffix();
        long employeeId = insertEmployee(duplicateCode, "Employee One");
        assertNotNull(employeeId);

        mockMvc.perform(get("/api/admin/employees").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").exists());

        mockMvc.perform(post("/api/admin/employees")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"EMP-NEW-" + suffix()
                                + "\",\"displayName\":\"Employee New\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/admin/employees")
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeCode\":\"" + duplicateCode
                                + "\",\"displayName\":\"Duplicate\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("EMPLOYEE_CODE_EXISTS"));
    }

    private long insertAccount(String email) {
        Long customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM customers WHERE email = ?", Long.class, email);
        jdbcTemplate.update(
                "INSERT INTO game_accounts (customer_id, name, nocape, status) VALUES (?, 'Test account', false, 'ACTIVE')",
                customerId);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM game_accounts", Long.class);
    }

    private long insertCode(long accountId, String code) {
        jdbcTemplate.update(
                "INSERT INTO invitation_codes (game_account_id, code) VALUES (?, ?)",
                accountId,
                code);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM invitation_codes", Long.class);
    }

    private long insertEmployee(String code, String name) {
        jdbcTemplate.update(
                "INSERT INTO employees (employee_code, display_name, work_status) VALUES (?, ?, 'ACTIVE')",
                code,
                name);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM employees WHERE employee_code = ?", Long.class, code);
    }

    private String insertCustomer(String suffix) {
        String email = suffix + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO customers (email, password_hash, display_name, status) VALUES (?, 'not-a-real-hash', ?, 'ACTIVE')",
                email,
                "Customer " + suffix);
        return email;
    }

    private RequestPostProcessor customer(String email) {
        return user(email).roles("CUSTOMER");
    }

    private RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
