package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpeakingMessageRequest(
        @NotBlank @Size(max = 4000) String content
) {
}
