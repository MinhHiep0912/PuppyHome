package com.puppyshome.backend.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public AdminUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AdminEntity admin = adminRepository.findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "Admin account not found"
                        )
                );

        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .disabled(!admin.isActive())
                .authorities("ROLE_ADMIN")
                .build();
    }
}