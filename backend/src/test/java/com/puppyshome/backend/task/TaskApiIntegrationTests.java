package com.puppyshome.backend.task;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void adminTransitionToInProgressCreatesOpenTask() throws Exception {
        String owner = "task-owner-" + suffix() + "@example.com";
        TestUsers.ensureCustomer(jdbcTemplate, owner);
        long accountId = insertAccount(owner);
        long serviceId = insertService();
        long requestId = createRequest(owner, serviceId, accountId);

        mockMvc.perform(get("/api/tasks").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)]".formatted(requestId))
                        .value(org.hamcrest.Matchers.hasSize(0)));

        mockMvc.perform(patch("/api/service-requests/{id}/status", requestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)]".formatted(requestId))
                        .value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)].status".formatted(requestId),
                        org.hamcrest.Matchers.contains("OPEN")))
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)].title".formatted(requestId),
                        org.hamcrest.Matchers.contains("Task service")));
    }

    @Test
    @Transactional
    void employeeClaimConflictAndHistory() throws Exception {
        long[] ids = setupInProgressRequest("claim");
        long taskId = ids[0];
        String employeeA = "task-emp-a-" + suffix();
        String employeeB = "task-emp-b-" + suffix();
        TestUsers.ensureEmployee(jdbcTemplate, employeeA, "Nhân viên A");
        long employeeAId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class, employeeA);
        TestUsers.ensureEmployee(jdbcTemplate, employeeB, "Nhân viên B");

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .with(employee(employeeA)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentAssigneeId").value(employeeAId))
                .andExpect(jsonPath("$.assigneeDisplayName").value("Nhân viên A"));

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .with(employee(employeeB)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("TASK_ALREADY_CLAIMED"));

        mockMvc.perform(get("/api/tasks/{id}/history", taskId)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("CLAIM"))
                .andExpect(jsonPath("$[0].actorId").value(employeeAId))
                .andExpect(jsonPath("$[0].fromAssigneeId")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].toAssigneeId").value(employeeAId));
    }

    @Test
    @Transactional
    void adminReassignWritesFullHistory() throws Exception {
        long[] ids = setupInProgressRequest("reassign");
        long taskId = ids[0];
        String employeeA = "task-emp-ra-" + suffix();
        String employeeB = "task-emp-rb-" + suffix();
        TestUsers.ensureEmployee(jdbcTemplate, employeeA, "Người A");
        long employeeAId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class, employeeA);
        long employeeBId = TestUsers.ensureEmployee(
                jdbcTemplate, employeeB, "Người B");

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .with(employee(employeeA)).with(csrf()))
                .andExpect(status().isOk());

        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = 'task-admin'",
                Long.class);

        mockMvc.perform(post("/api/tasks/{id}/reassign", taskId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeUserId\": %d, \"reason\": \"Đổi người phụ trách\"}"
                                .formatted(employeeBId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAssigneeId").value(employeeBId));

        mockMvc.perform(get("/api/tasks/{id}/history", taskId)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].action").value("REASSIGN"))
                .andExpect(jsonPath("$[1].actorId").value(adminId))
                .andExpect(jsonPath("$[1].fromAssigneeId").value(employeeAId))
                .andExpect(jsonPath("$[1].toAssigneeId").value(employeeBId))
                .andExpect(jsonPath("$[1].reason").value("Đổi người phụ trách"));
    }

    @Test
    @Transactional
    void releaseAndRequestDoneSyncWritesHistory() throws Exception {
        long[] ids = setupInProgressRequest("release");
        long taskId = ids[0];
        long requestId = ids[1];
        String employee = "task-emp-rel-" + suffix();
        TestUsers.ensureEmployee(jdbcTemplate, employee, "Người Release");
        long employeeId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class, employee);

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .with(employee(employee)).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/{id}/release", taskId)
                        .with(employee(employee)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.currentAssigneeId")
                        .value(org.hamcrest.Matchers.nullValue()));

        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = 'task-admin'",
                Long.class);

        mockMvc.perform(patch("/api/service-requests/{id}/status", requestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{id}", taskId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/tasks/{id}/history", taskId)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].action").value("RELEASE"))
                .andExpect(jsonPath("$[1].actorId").value(employeeId))
                .andExpect(jsonPath("$[2].action").value("COMPLETE"))
                .andExpect(jsonPath("$[2].actorId").value(adminId));
    }

    @Test
    @Transactional
    void requestCancelSyncCancelsTask() throws Exception {
        long[] ids = setupInProgressRequest("cancel");
        long taskId = ids[0];
        long requestId = ids[1];

        mockMvc.perform(patch("/api/service-requests/{id}/status", requestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{id}", taskId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/tasks/{id}/history", taskId)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CANCELLED"));
    }

    @Test
    @Transactional
    void employeeScopeAndPermissionRules() throws Exception {
        long[] ids = setupInProgressRequest("scope");
        long taskId = ids[0];
        String employeeA = "task-emp-sa-" + suffix();
        String employeeB = "task-emp-sb-" + suffix();
        TestUsers.ensureEmployee(jdbcTemplate, employeeA, "Người A");
        TestUsers.ensureEmployee(jdbcTemplate, employeeB, "Người B");

        mockMvc.perform(get("/api/tasks").with(employee(employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)]".formatted(ids[1]))
                        .value(org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .with(employee(employeeA)).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks").with(employee(employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.serviceRequestId == %d)]".formatted(ids[1]))
                        .value(org.hamcrest.Matchers.hasSize(0)));

        mockMvc.perform(get("/api/tasks/{id}", taskId).with(employee(employeeB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/tasks/{id}/release", taskId)
                        .with(employee(employeeB)).with(csrf()))
                .andExpect(status().isNotFound());

        String owner = "task-customer-" + suffix() + "@example.com";
        TestUsers.ensureCustomer(jdbcTemplate, owner);
        mockMvc.perform(get("/api/tasks").with(customer(owner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/tasks/{id}/claim", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks/{id}/reassign", taskId)
                        .with(employee(employeeA)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeUserId\": 1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/tasks/{id}/history", taskId)
                        .with(admin()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/{id}/history", taskId)
                        .with(admin()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void statusFilterAndTaskNotFound() throws Exception {
        long[] ids = setupInProgressRequest("filter");
        long taskId = ids[0];

        mockMvc.perform(get("/api/tasks?status=OPEN").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.id == %d)]".formatted(taskId))
                        .value(org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(get("/api/tasks?status=DONE").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.id == %d)]".formatted(taskId))
                        .value(org.hamcrest.Matchers.hasSize(0)));

        mockMvc.perform(get("/api/tasks/999999999").with(admin()))
                .andExpect(status().isNotFound());
    }

    private long[] setupInProgressRequest(String tag) throws Exception {
        String owner = "task-" + tag + "-" + suffix() + "@example.com";
        TestUsers.ensureCustomer(jdbcTemplate, owner);
        long accountId = insertAccount(owner);
        long serviceId = insertService();
        long requestId = createRequest(owner, serviceId, accountId);
        TestUsers.ensureUser(jdbcTemplate, "task-admin", "ADMIN");

        mockMvc.perform(patch("/api/service-requests/{id}/status", requestId)
                        .with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        Long taskId = jdbcTemplate.queryForObject(
                "SELECT id FROM tasks WHERE service_request_id = ?",
                Long.class,
                requestId);
        return new long[]{taskId, requestId};
    }

    private long insertAccount(String ownerEmail) {
        Long customerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE login_identifier = ?",
                Long.class, ownerEmail);
        jdbcTemplate.update(
                "INSERT INTO game_accounts (customer_id, name, nocape, status) VALUES (?, ?, false, 'ACTIVE')",
                customerId,
                "Account " + suffix());
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM game_accounts WHERE customer_id = ?",
                Long.class,
                customerId);
    }

    private long insertService() {
        String slug = "task-service-" + suffix();
        jdbcTemplate.update(
                """
                        INSERT INTO services (
                            name, slug, status, sort_order,
                            price_amount, price_currency, price_unit)
                        VALUES ('Task service', ?, 'PUBLISHED', 1, 1000, 'VND', 'day')
                        """,
                slug);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM services WHERE slug = ?",
                Long.class,
                slug);
    }

    private long createRequest(String owner, long serviceId, long accountId)
            throws Exception {
        var result = mockMvc.perform(post("/api/service-requests")
                        .with(customer(owner)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\": %d, \"gameAccountId\": %d}"
                                .formatted(serviceId, accountId)))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(
                result.getResponse().getHeader("Location")
                        .replace("/api/service-requests/", ""));
    }

    private RequestPostProcessor admin() {
        TestUsers.ensureUser(jdbcTemplate, "task-admin", "ADMIN");
        return user("task-admin").roles("ADMIN");
    }

    private RequestPostProcessor employee(String code) {
        return user(code).roles("EMPLOYEE");
    }

    private RequestPostProcessor customer(String email) {
        return user(email).roles("CUSTOMER");
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
