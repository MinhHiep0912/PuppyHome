package com.puppyshome.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository
        extends JpaRepository<AdminEntity, Long> {

    Optional<AdminEntity> findByUsername(String username);
}