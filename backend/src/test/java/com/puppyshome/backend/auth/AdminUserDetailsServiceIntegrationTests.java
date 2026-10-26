package com.puppyshome.backend.auth;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AdminUserDetailsServiceIntegrationTests {

    @Autowired
    private AdminUserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void loadsActiveAdminWithEncodedPasswordAndRole() {
        String username = testUsername("active");
        String rawPassword = "Test-Password-2026!";
        String passwordHash = passwordEncoder.encode(rawPassword);

        insertAdmin(username, passwordHash, true);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);

        assertEquals(username, userDetails.getUsername());
        assertEquals(passwordHash, userDetails.getPassword());
        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        userDetails.getPassword()
                )
        );
        assertTrue(userDetails.isEnabled());
        assertTrue(
                userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("ROLE_ADMIN"::equals)
        );
    }

    @Test
    @Transactional
    void marksInactiveAdminAsDisabled() {
        String username = testUsername("inactive");
        String passwordHash =
                passwordEncoder.encode("Test-Password-2026!");

        insertAdmin(username, passwordHash, false);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void throwsWhenUsernameDoesNotExist() {
        String username = testUsername("missing");

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(username)
        );
    }

    @Test
    void bcryptMatchesCorrectPasswordButRejectsWrongPassword() {
        String rawPassword = "Test-Password-2026!";
        String passwordHash = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, passwordHash);
        assertTrue(passwordEncoder.matches(rawPassword, passwordHash));
        assertFalse(
                passwordEncoder.matches(
                        "Wrong-Password-2026!",
                        passwordHash
                )
        );
    }

    private void insertAdmin(
            String username,
            String passwordHash,
            boolean active
    ) {
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
                active
        );
    }

    private String testUsername(String prefix) {
        return "test-" + prefix + "-"
                + UUID.randomUUID().toString().replace("-", "");
    }
}