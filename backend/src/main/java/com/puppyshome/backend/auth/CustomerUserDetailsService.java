package com.puppyshome.backend.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        CustomerEntity customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Customer account not found"));

        return User.withUsername(customer.getEmail())
                .password(customer.getPasswordHash())
                .disabled(!customer.isActive())
                .authorities("ROLE_CUSTOMER")
                .build();
    }
}
