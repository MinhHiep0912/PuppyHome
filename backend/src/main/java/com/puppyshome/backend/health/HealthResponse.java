package com.puppyshome.backend.health;

public record HealthResponse(
        String status,
        String application
) {
}