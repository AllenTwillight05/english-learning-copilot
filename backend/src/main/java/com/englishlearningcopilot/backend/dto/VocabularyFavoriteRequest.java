package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotNull;

public record VocabularyFavoriteRequest(
        @NotNull Long vocabularyId
) {
}
