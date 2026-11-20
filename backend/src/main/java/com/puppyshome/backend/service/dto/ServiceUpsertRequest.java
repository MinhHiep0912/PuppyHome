package com.puppyshome.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceUpsertRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 180)
        String slug,

        @Size(max = 500)
        String shortDescription,

        String description,

        @Size(max = 2048)
        String imageUrl,

        @NotNull
        Integer sortOrder
) {
}
