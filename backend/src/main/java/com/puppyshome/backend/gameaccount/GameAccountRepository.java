package com.puppyshome.backend.gameaccount;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameAccountRepository
        extends JpaRepository<GameAccountEntity, Long> {

    List<GameAccountEntity> findAllByCustomer_IdOrderByIdAsc(Long customerId);

    List<GameAccountEntity> findAllByOrderByIdAsc();
}
