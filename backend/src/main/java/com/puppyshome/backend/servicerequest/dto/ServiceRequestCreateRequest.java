package com.puppyshome.backend.servicerequest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ServiceRequestCreateRequest(
        @NotNull
        Long serviceId,

        @NotNull
        Long gameAccountId,

        LocalDateTime requestedStartAt,

        String notes
) {
}
