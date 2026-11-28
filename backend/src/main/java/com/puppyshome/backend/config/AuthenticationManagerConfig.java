package com.puppyshome.backend.config;

import com.puppyshome.backend.auth.AppUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class AuthenticationManagerConfig {

    @Bean
    AuthenticationManager authenticationManager(
            AppUserDetailsService appUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(appUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(provider));
    }
}
