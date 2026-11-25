package com.puppyshome.backend.servicerequest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceRequestResponse(
        Long id,
        Long customerId,
        Long gameAccountId,
        Long serviceId,
        String snapshotServiceName,
        BigDecimal snapshotPriceAmount,
        String snapshotCurrency,
        String snapshotPriceUnit,
        String status,
        LocalDateTime requestedStartAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int durationDays,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
