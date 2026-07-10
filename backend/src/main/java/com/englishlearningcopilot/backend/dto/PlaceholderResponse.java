package com.englishlearningcopilot.backend.dto;

public record PlaceholderResponse(
        String resource,
        String operation,
        String message
) {
}
