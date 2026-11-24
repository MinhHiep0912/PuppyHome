package com.puppyshome.backend.invitationcode;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCodeRepository extends JpaRepository<InvitationCodeEntity, Long> {
    List<InvitationCodeEntity> findAllByGameAccount_IdAndDeletedAtIsNullOrderByIdAsc(Long gameAccountId);
    Optional<InvitationCodeEntity> findByIdAndDeletedAtIsNull(Long id);
}
