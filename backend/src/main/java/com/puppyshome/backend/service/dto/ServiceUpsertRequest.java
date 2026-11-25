package com.puppyshome.backend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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
        Integer sortOrder,

        @DecimalMin(value = "0")
        @Digits(integer = 12, fraction = 0)
        BigDecimal priceAmount,

        @Pattern(regexp = "[A-Z]{3}")
        String priceCurrency,

        @Size(max = 32)
        String priceUnit
) {
}
