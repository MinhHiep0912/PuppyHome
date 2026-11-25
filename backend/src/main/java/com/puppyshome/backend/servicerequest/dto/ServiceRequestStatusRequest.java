package com.puppyshome.backend.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ServiceRequestStatusRequest(
        @NotBlank
        @Pattern(regexp = "IN_PROGRESS|DONE|CANCELLED")
        String status
) {
}
