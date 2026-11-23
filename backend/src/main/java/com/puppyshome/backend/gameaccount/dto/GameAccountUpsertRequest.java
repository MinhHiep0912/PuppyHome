package com.puppyshome.backend.gameaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameAccountUpsertRequest(
        @NotBlank
        @Size(max = 128)
        String name,

        Boolean nocape
) {
}
