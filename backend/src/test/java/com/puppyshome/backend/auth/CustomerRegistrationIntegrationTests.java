package com.puppyshome.backend.auth;

import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerRegistrationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void registrationCreatesActiveSessionAndAllowsImmediateLogin()
            throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String password = "Abcdef12";
        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult registration = mockMvc.perform(
                post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) registration.getRequest().getSession(false);
        assertNotNull(session);
        Object savedContext = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertTrue(savedContext instanceof SecurityContext);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CUSTOMER"));

        mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CUSTOMER"));
    }

    @Test
    @Transactional
    void duplicateEmailReturnsConflict() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(
                post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "Abcdef12")))
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "Abcdef12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("EMAIL_EXISTS"));
    }

    @Test
    void invalidDataReturnsBadRequest() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(
                post("/api/auth/register")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return cookie;
    }

    private String registerJson(String email, String password) {
        return """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);
    }

    private String loginJson(String email, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);
    }
}
