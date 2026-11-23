package com.puppyshome.backend.gameaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GameAccountStatusRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVE|ARCHIVED")
        String status
) {
}
