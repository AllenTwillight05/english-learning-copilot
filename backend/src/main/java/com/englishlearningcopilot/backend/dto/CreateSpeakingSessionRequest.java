package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpeakingSessionRequest(
        @NotBlank String scenarioId,
        @Size(max = 200) String selectedTopic
) {
}
