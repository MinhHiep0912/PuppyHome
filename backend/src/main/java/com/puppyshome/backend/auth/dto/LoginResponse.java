package com.puppyshome.backend.auth.dto;

import java.util.List;

public record LoginResponse(
        String username,
        List<String> authorities
) {
}