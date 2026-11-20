package com.puppyshome.backend.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminServiceApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminServiceRoutesRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/services"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/services")
                        .with(user("employee").authorities(
                                () -> "ROLE_EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void writeRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/admin/services")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validServiceJson("csrf-service")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void adminCanManageAndSoftDeleteServiceWithoutBreakingExistingReference()
            throws Exception {
        String suffix = randomSuffix();
        String initialSlug = "admin-service-" + suffix;
        String updatedSlug = "admin-service-updated-" + suffix;

        MvcResult createResult = mockMvc.perform(
                        post("/api/admin/services")
                                .with(admin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validServiceJson(initialSlug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        long serviceId = Long.parseLong(
                createResult.getResponse()
                        .getHeader("Location")
                        .replace("/api/admin/services/", ""));

        long priceTableId = insertPriceTable(suffix);
        jdbcTemplate.update(
                """
                        INSERT INTO price_table_items (
                            price_table_id,
                            service_id,
                            item_name
                        )
                        VALUES (?, ?, ?)
                        """,
                priceTableId,
                serviceId,
                "Referenced service item");

        mockMvc.perform(get("/api/admin/services").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(initialSlug)));

        mockMvc.perform(
                        put("/api/admin/services/{id}", serviceId)
                                .with(admin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validServiceJson(updatedSlug)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(updatedSlug))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(
                        patch("/api/admin/services/{id}/status", serviceId)
                                .with(admin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(updatedSlug)));

        mockMvc.perform(
                        delete("/api/admin/services/{id}", serviceId)
                                .with(admin())
                                .with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(
                "DRAFT",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM services WHERE id = ?",
                        String.class,
                        serviceId));
        assertEquals(
                serviceId,
                jdbcTemplate.queryForObject(
                        """
                                SELECT service_id
                                FROM price_table_items
                                WHERE price_table_id = ?
                                """,
                        Long.class,
                        priceTableId));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(updatedSlug))));
    }

    @Test
    @Transactional
    void invalidInputAndDuplicateSlugAreRejected() throws Exception {
        String slug = "duplicate-service-" + randomSuffix();

        mockMvc.perform(post("/api/admin/services")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validServiceJson(slug)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/services")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validServiceJson(slug)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/services")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": " ",
                                    "slug": "invalid-service",
                                    "sortOrder": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingServiceReturnsNotFound() throws Exception {
        mockMvc.perform(
                        delete("/api/admin/services/{id}", Long.MAX_VALUE)
                                .with(admin())
                                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    private long insertPriceTable(String suffix) {
        jdbcTemplate.update(
                """
                        INSERT INTO price_tables (title, slug)
                        VALUES (?, ?)
                        """,
                "Referenced item table",
                "referenced-table-" + suffix);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM price_tables WHERE slug = ?",
                Long.class,
                "referenced-table-" + suffix);
    }

    private String validServiceJson(String slug) {
        return """
                {
                    "name": "Admin API Service",
                    "slug": "%s",
                    "shortDescription": "Service description",
                    "description": "Integration test service",
                    "imageUrl": null,
                    "sortOrder": 10
                }
                """.formatted(slug);
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
