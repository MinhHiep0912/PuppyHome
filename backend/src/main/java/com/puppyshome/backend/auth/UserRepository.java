package com.puppyshome.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLoginIdentifier(String loginIdentifier);

    boolean existsByLoginIdentifier(String loginIdentifier);
}
