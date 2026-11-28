package com.puppyshome.backend.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginIdentifier)
            throws UsernameNotFoundException {

        UserEntity user = userRepository
                .findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User account not found"));

        return User.withUsername(user.getLoginIdentifier())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .authorities(user.getRoles()
                        .stream()
                        .map(role -> "ROLE_" + role.getCode())
                        .toArray(String[]::new))
                .build();
    }
}
