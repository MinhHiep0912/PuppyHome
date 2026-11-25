package com.puppyshome.backend.service.dto;

import java.math.BigDecimal;

public record PublicServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String imageUrl,
        int sortOrder,
        BigDecimal priceAmount,
        String priceCurrency,
        String priceUnit
) {
}