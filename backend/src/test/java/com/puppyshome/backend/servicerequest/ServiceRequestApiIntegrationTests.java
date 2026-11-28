package com.puppyshome.backend.servicerequest;

import com.puppyshome.backend.support.TestUsers;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceRequestApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesRequestSchemaAndRestrictiveForeignKeys() {
        Integer requestColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'service_requests'
                          AND COLUMN_NAME IN (
                              'customer_id', 'game_account_id', 'service_id',
                              'snapshot_service_name', 'snapshot_price_amount',
                              'snapshot_currency', 'snapshot_price_unit',
                              'status_actor_type', 'status_actor_id',
                              'requested_start_at', 'start_at', 'end_at',
                              'duration_days', 'notes')
                        """,
                Integer.class);
        assertEquals(14, requestColumns);

        Integer restrictiveForeignKeys = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
                        WHERE CONSTRAINT_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'service_requests'
                          AND DELETE_RULE = 'RESTRICT'
                        """,
                Integer.class);
        assertEquals(3, restrictiveForeignKeys);

        Integer priceColumns = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'services'
                          AND COLUMN_NAME IN (
                              'price_amount', 'price_currency', 'price_unit')
                        """,
                Integer.class);
        assertEquals(3, priceColumns);
    }

    @Test
    @Transactional
    void customerCreatesRequestWithServerOwnedValuesAndImmutablePriceSnapshot()
            throws Exception {
        String owner = insertCustomer("request-owner-" + suffix());
        String other = insertCustomer("request-other-" + suffix());
        long accountId = insertAccount(owner, "ACTIVE");
        long serviceId = insertService("Service snapshot", "200000", "VND", "day", "PUBLISHED");

        String requestJson = """
                {
                    "serviceId": %d,
                    "gameAccountId": %d,
                    "requestedStartAt": "2026-12-01T10:30:00",
                    "notes": "  Khách ghi chú  ",
                    "customerId": 999999,
                    "status": "DONE",
                    "snapshotPriceAmount": 1,
                    "startAt": "2000-01-01T00:00:00",
                    "endAt": "2000-01-02T00:00:00"
                }
                """.formatted(serviceId, accountId);

        var result = mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNumber())
                .andExpect(jsonPath("$.gameAccountId").value(accountId))
                .andExpect(jsonPath("$.serviceId").value(serviceId))
                .andExpect(jsonPath("$.snapshotServiceName").value("Service snapshot"))
                .andExpect(jsonPath("$.snapshotPriceAmount").value(200000))
                .andExpect(jsonPath("$.snapshotCurrency").value("VND"))
                .andExpect(jsonPath("$.snapshotPriceUnit").value("day"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.notes").value("Khách ghi chú"))
                .andReturn();

        long requestId = Long.parseLong(
                result.getResponse().getHeader("Location")
                        .replace("/api/service-requests/", ""));
        Long customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class,
                owner);
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM service_requests
                        WHERE id = ? AND customer_id = ?
                          AND status = 'NEW'
                          AND status_actor_id IS NULL
                          AND start_at = created_at
                          AND end_at = DATE_ADD(start_at, INTERVAL duration_days DAY)
                          AND duration_days = 30
                        """,
                Integer.class,
                requestId,
                customerId));

        jdbcTemplate.update(
                "UPDATE services SET name = 'Changed name', price_amount = 999 WHERE id = ?",
                serviceId);
        mockMvc.perform(get("/api/service-requests/{id}", requestId)
                        .with(customer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotServiceName").value("Service snapshot"))
                .andExpect(jsonPath("$.snapshotPriceAmount").value(200000));

        mockMvc.perform(get("/api/service-requests/{id}", requestId)
                        .with(customer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void enforcesAvailabilityOwnershipCsrfAndRequestPermissions() throws Exception {
        String owner = insertCustomer("request-rules-" + suffix());
        String other = insertCustomer("request-other-rules-" + suffix());
        long ownerAccount = insertAccount(owner, "ACTIVE");
        long otherAccount = insertAccount(other, "ACTIVE");
        long archivedAccount = insertAccount(owner, "ARCHIVED");
        long serviceId = insertService("Available", "0", "VND", "service", "PUBLISHED");
        long draftService = insertService("Draft", "100", "VND", "service", "DRAFT");
        long missingPrice = insertService("No price", null, "VND", "service", "PUBLISHED");

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": \"not-an-id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(serviceId, ownerAccount)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(serviceId, otherAccount)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(serviceId, archivedAccount)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("ACCOUNT_NOT_ACTIVE"));

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(draftService, ownerAccount)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("SERVICE_NOT_AVAILABLE"));

        mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(missingPrice, ownerAccount)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("PRICE_NOT_AVAILABLE"));

        mockMvc.perform(get("/api/service-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void customerAndAdminTransitionsRespectStateMachineAndRecordActor()
            throws Exception {
        String owner = insertCustomer("request-status-" + suffix());
        String other = insertCustomer("request-status-other-" + suffix());
        long accountId = insertAccount(owner, "ACTIVE");
        long serviceId = insertService("Status service", "1000", "VND", "day", "PUBLISHED");
        long requestId = createRequest(owner, serviceId, accountId);
        insertAdmin("request-admin");

        mockMvc.perform(patch("/api/service-requests/{id}/status", requestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(post("/api/service-requests/{id}/cancel", requestId)
                        .with(customer(other)).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/service-requests/{id}/cancel", requestId)
                        .with(customer(owner)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals("CUSTOMER", jdbcTemplate.queryForObject(
                "SELECT status_actor_type FROM service_requests WHERE id = ?",
                String.class,
                requestId));
        assertEquals(jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE login_identifier = ?",
                        Long.class,
                        owner),
                jdbcTemplate.queryForObject(
                        "SELECT status_actor_id FROM service_requests WHERE id = ?",
                        Long.class,
                        requestId));

        mockMvc.perform(post("/api/service-requests/{id}/cancel", requestId)
                        .with(customer(owner)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("INVALID_STATUS_TRANSITION"));

        long secondRequestId = createRequest(owner, serviceId, accountId);
        mockMvc.perform(patch("/api/service-requests/{id}/status", secondRequestId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/service-requests/{id}/status", secondRequestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/service-requests/{id}/status", secondRequestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        assertEquals("ADMIN", jdbcTemplate.queryForObject(
                "SELECT status_actor_type FROM service_requests WHERE id = ?",
                String.class,
                secondRequestId));
        assertEquals(jdbcTemplate.queryForObject(
                        "SELECT id FROM users WHERE login_identifier = 'request-admin'",
                        Long.class),
                jdbcTemplate.queryForObject(
                        "SELECT status_actor_id FROM service_requests WHERE id = ?",
                        Long.class,
                        secondRequestId));

        mockMvc.perform(patch("/api/service-requests/{id}/status", secondRequestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(get("/api/service-requests").with(customer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        Long ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class,
                owner);
        mockMvc.perform(get("/api/service-requests").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerId == %d)]".formatted(ownerId))
                        .value(org.hamcrest.Matchers.hasSize(2)));
    }

    private long createRequest(String email, long serviceId, long accountId)
            throws Exception {
        var result = mockMvc.perform(post("/api/service-requests")
                        .with(customer(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(serviceId, accountId)))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(
                result.getResponse().getHeader("Location")
                        .replace("/api/service-requests/", ""));
    }

    private long insertService(
            String name,
            String amount,
            String currency,
            String unit,
            String status) {
        String slug = "request-service-" + suffix();
        jdbcTemplate.update(
                """
                        INSERT INTO services (
                            name, slug, status, sort_order,
                            price_amount, price_currency, price_unit)
                        VALUES (?, ?, ?, 1, ?, ?, ?)
                        """,
                name,
                slug,
                status,
                amount == null ? null : new java.math.BigDecimal(amount),
                currency,
                unit);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM services WHERE slug = ?",
                Long.class,
                slug);
    }

    private long insertAccount(String email, String status) {
        Long customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?", Long.class, email);
        jdbcTemplate.update(
                "INSERT INTO game_accounts (customer_id, name, nocape, status) VALUES (?, ?, false, ?)",
                customerId,
                "Account " + suffix(),
                status);
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM game_accounts WHERE customer_id = ?",
                Long.class,
                customerId);
    }

    private String insertCustomer(String suffix) {
        String email = suffix + "@example.com";
        TestUsers.ensureCustomer(jdbcTemplate, email);
        return email;
    }

    private void insertAdmin(String username) {
        TestUsers.ensureUser(jdbcTemplate, username, "ADMIN");
    }

    private String createJson(long serviceId, long accountId) {
        return """
                {"serviceId": %d, "gameAccountId": %d}
                """.formatted(serviceId, accountId);
    }

    private RequestPostProcessor customer(String email) {
        return user(email).roles("CUSTOMER");
    }

    private RequestPostProcessor admin() {
        TestUsers.ensureUser(jdbcTemplate, "request-admin", "ADMIN");
        return user("request-admin").roles("ADMIN");
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
