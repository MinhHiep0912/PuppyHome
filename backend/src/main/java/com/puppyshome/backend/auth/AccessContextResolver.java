package com.puppyshome.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccessContextResolver {

    private final CustomerRepository customerRepository;

    public AccessContextResolver(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public AccessContext resolve(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        if (admin) {
            return new AccessContext(null, true);
        }

        CustomerEntity customer = customerRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy Customer."));
        return new AccessContext(customer.getId(), false);
    }
}
