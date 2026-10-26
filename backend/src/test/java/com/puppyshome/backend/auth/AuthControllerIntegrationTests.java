package com.puppyshome.backend.auth;

import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void successfulLoginReturnsSafeResponseAndSavesSession()
            throws Exception {

        String username = testUsername();
        String password = "Test-Password-2026!";

        insertAdmin(username, passwordEncoder.encode(password), true);

        Cookie csrfCookie = fetchCsrfCookie();

        var result = mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);

        assertNotNull(session, "Login thành công phải tạo session");

        Object savedContext = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        assertNotNull(savedContext, "Session phải lưu SecurityContext");
        assertTrue(savedContext instanceof SecurityContext);

        SecurityContext securityContext = (SecurityContext) savedContext;

        assertEquals(
                username,
                securityContext.getAuthentication().getName());
    }

    @Test
    @Transactional
    void incorrectPasswordReturnsUnauthorized() throws Exception {
        String username = testUsername();

        insertAdmin(
                username,
                passwordEncoder.encode("Correct-Password-2026!"),
                true);

        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(
                                username,
                                "Wrong-Password-2026!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail")
                        .value("Thông tin đăng nhập không hợp lệ."));
    }

    @Test
    @Transactional
    void disabledAdminCannotLogin() throws Exception {
        String username = testUsername();
        String password = "Test-Password-2026!";

        insertAdmin(
                username,
                passwordEncoder.encode(password),
                false);

        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail")
                        .value("Thông tin đăng nhập không hợp lệ."));
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(
                                "test-user",
                                "Test-Password-2026!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginRejectsBlankCredentials() throws Exception {
        Cookie csrfCookie = fetchCsrfCookie();

        mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": " ",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Username và password không được để trống."));
    }

    @Test
    @Transactional
    void authenticatedSessionCanReadCurrentAdmin() throws Exception {
        String username = testUsername();
        String password = "Test-Password-2026!";

        insertAdmin(username, passwordEncoder.encode(password), true);

        Cookie csrfCookie = fetchCsrfCookie();
        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(
                get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.authorities").isArray());
    }

    @Test
    void currentAdminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void logoutInvalidatesSessionAndRequiresAuthenticationAgain()
            throws Exception {

        String username = testUsername();
        String password = "Test-Password-2026!";

        insertAdmin(
                username,
                passwordEncoder.encode(password),
                true);

        Cookie csrfCookie = fetchCsrfCookie();

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header(
                                "X-XSRF-TOKEN",
                                csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(
                post("/api/auth/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header(
                                "X-XSRF-TOKEN",
                                csrfCookie.getValue()))
                .andExpect(status().isNoContent());

        assertTrue(
                session.isInvalid(),
                "Session phải bị hủy sau khi đăng xuất.");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie fetchCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");

        assertNotNull(
                csrfCookie,
                "GET /api/auth/csrf phải trả cookie XSRF-TOKEN");

        return csrfCookie;
    }

    private void insertAdmin(
            String username,
            String passwordHash,
            boolean active) {
        jdbcTemplate.update(
                """
                        INSERT INTO admins (
                                username,
                                password_hash,
                                display_name,
                                is_active
                        )
                        VALUES (?, ?, ?, ?)
                        """,
                username,
                passwordHash,
                "Test Admin",
                active);
    }

    @Test
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    private String testUsername() {
        return "test-http-login-"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String loginJson(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
    }
}