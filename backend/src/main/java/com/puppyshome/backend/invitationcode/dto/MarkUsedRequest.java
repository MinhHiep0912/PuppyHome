package com.puppyshome.backend.invitationcode.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarkUsedRequest(
        @NotNull(message = "employeeId là bắt buộc.")
        @Positive(message = "employeeId phải là số dương.")
        Long employeeId) {
}
