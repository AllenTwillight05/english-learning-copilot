package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VocabularyRatingRequest(
        @NotNull Long vocabularyId,
        @NotNull @Min(1) @Max(4) Integer score
) {
}
