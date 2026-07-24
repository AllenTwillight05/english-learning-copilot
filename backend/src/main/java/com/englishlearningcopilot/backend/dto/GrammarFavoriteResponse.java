package com.englishlearningcopilot.backend.dto;

public record GrammarFavoriteResponse(
        Integer grammarQuestionId,
        boolean favorited
) {
}
