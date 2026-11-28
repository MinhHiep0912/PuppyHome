package com.puppyshome.backend.auth;

import java.util.UUID;

import com.puppyshome.backend.support.TestUsers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthenticationManagerIntegrationTests {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void authenticatesActiveAdminWithCorrectPassword() {
        String username = testUsername();
        String rawPassword = "Test-Password-2026!";

        insertAdmin(
                username,
                passwordEncoder.encode(rawPassword),
                true
        );

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                rawPassword
                        )
                );

        assertTrue(authentication.isAuthenticated());
        assertEquals(username, authentication.getName());
        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals)
        );
    }

    @Test
    @Transactional
    void rejectsIncorrectPassword() {
        String username = testUsername();

        insertAdmin(
                username,
                passwordEncoder.encode("Correct-Password-2026!"),
                true
        );

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                "Wrong-Password-2026!"
                        )
                )
        );
    }

    @Test
    @Transactional
    void rejectsDisabledAdmin() {
        String username = testUsername();
        String rawPassword = "Test-Password-2026!";

        insertAdmin(
                username,
                passwordEncoder.encode(rawPassword),
                false
        );

        assertThrows(
                DisabledException.class,
                () -> authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                rawPassword
                        )
                )
        );
    }

    private void insertAdmin(
            String username,
            String passwordHash,
            boolean active
    ) {
        TestUsers.ensureUser(
                jdbcTemplate,
                username,
                "ADMIN",
                passwordHash,
                active ? "ACTIVE" : "DISABLED");
    }

    private String testUsername() {
        return "test-login-"
                + UUID.randomUUID().toString().replace("-", "");
    }
}