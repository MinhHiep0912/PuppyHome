package com.puppyshome.backend.gameaccount.dto;

import java.time.LocalDateTime;

public record GameAccountResponse(
        Long id,
        Long customerId,
        String name,
        boolean nocape,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
