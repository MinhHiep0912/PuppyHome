package com.puppyshome.backend.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository) throws Exception {

        http
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(
                        securityContextRepository))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(
                                (request, response, authentication) -> response.setStatus(
                                        HttpServletResponse.SC_NO_CONTENT)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health",
                                "/api/services",
                                "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me")
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/logout")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/game-accounts",
                                "/api/game-accounts/**")
                        .authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/game-accounts/*/invitation-codes")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/invitation-codes/*/mark-used")
                        .hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/invitation-codes/*")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/invitation-codes/*")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(
                                "/api/admin/employees",
                                "/api/admin/employees/**")
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/game-accounts",
                                "/api/game-accounts/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(
                                "/api/admin/services",
                                "/api/admin/services/**")
                        .hasRole("ADMIN")
                        .anyRequest().denyAll());

        return http.build();
    }
}