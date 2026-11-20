package com.puppyshome.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ServiceStatusRequest(
        @NotBlank
        @Pattern(regexp = "DRAFT|PUBLISHED")
        String status
) {
}
