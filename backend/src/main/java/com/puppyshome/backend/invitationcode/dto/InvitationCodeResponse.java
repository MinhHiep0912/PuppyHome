package com.puppyshome.backend.invitationcode.dto;

import java.time.LocalDateTime;

public record InvitationCodeResponse(
        Long id,
        Long gameAccountId,
        String code,
        String note,
        Long usedByEmployeeId,
        String usedByEmployeeName,
        LocalDateTime usedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
