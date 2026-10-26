package com.puppyshome.backend.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}