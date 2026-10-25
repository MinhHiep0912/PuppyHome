package com.puppyshome.backend.service.dto;

public record PublicServiceResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String imageUrl,
        int sortOrder
) {
}