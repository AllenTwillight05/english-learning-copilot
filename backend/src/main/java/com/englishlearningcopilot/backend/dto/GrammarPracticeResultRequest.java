package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotNull;

public record GrammarPracticeResultRequest(
        @NotNull Integer grammarQuestionId,
        @NotNull Boolean incorrect
) {
}
