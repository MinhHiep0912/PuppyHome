package com.puppyshome.backend.service.dto;

public record AdminServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String imageUrl,
        String status,
        int sortOrder
) {
}
