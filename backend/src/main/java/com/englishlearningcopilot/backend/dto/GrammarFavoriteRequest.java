package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.NotNull;

public record GrammarFavoriteRequest(
        @NotNull Integer grammarQuestionId
) {
}
