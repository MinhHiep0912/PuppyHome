package com.puppyshome.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfileEntity, Long> {
}
