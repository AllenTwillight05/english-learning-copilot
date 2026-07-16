package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSpeakingSessionRequest(
        @NotBlank String scenarioId
) {
}
