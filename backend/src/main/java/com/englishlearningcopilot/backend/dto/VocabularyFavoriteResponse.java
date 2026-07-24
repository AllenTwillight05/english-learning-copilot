package com.englishlearningcopilot.backend.dto;

public record VocabularyFavoriteResponse(
        Long vocabularyId,
        boolean favorited
) {
}
