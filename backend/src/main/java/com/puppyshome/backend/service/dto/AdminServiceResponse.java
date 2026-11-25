package com.puppyshome.backend.service.dto;

import java.math.BigDecimal;

public record AdminServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String imageUrl,
        String status,
        int sortOrder,
        BigDecimal priceAmount,
        String priceCurrency,
        String priceUnit
) {
}
