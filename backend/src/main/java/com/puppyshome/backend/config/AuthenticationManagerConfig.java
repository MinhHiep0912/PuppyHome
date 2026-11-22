package com.puppyshome.backend.config;

import java.util.List;

import com.puppyshome.backend.auth.AdminUserDetailsService;
import com.puppyshome.backend.auth.CustomerUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationManagerConfig {

    @Bean
    AuthenticationManager authenticationManager(
            AdminUserDetailsService adminUserDetailsService,
            CustomerUserDetailsService customerUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider adminProvider =
                new DaoAuthenticationProvider(adminUserDetailsService);
        adminProvider.setPasswordEncoder(passwordEncoder);

        DaoAuthenticationProvider customerProvider =
                new DaoAuthenticationProvider(customerUserDetailsService);
        customerProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(adminProvider, customerProvider));
    }
}