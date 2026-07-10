package com.englishlearningcopilot.backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
