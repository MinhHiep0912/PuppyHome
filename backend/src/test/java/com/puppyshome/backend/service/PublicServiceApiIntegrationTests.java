package com.puppyshome.backend.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicServiceApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void publicEndpointReturnsPublishedServicesAndHidesDrafts()
            throws Exception {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "");

        String publishedSlug = "test-published-" + suffix;
        String draftSlug = "test-draft-" + suffix;

        insertService(
                "Integration Test Published Service",
                publishedSlug,
                "PUBLISHED",
                100);

        insertService(
                "Integration Test Draft Service",
                draftSlug,
                "DRAFT",
                101);

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(publishedSlug)))
                .andExpect(content().string(
                        not(containsString(draftSlug))));
    }

    private void insertService(
            String name,
            String slug,
            String status,
            int sortOrder) {
        jdbcTemplate.update(
                """
                        INSERT INTO services (
                            name,
                            slug,
                            short_description,
                            description,
                            image_url,
                            status,
                            sort_order
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                name,
                slug,
                "Test description",
                "Integration test data",
                null,
                status,
                sortOrder);
    }
}