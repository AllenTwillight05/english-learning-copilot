package com.englishlearningcopilot.backend.dto;

public record VocabularyLeaderboardItemResponse(
        int rank,
        Long vocabularyId,
        String word,
        String briefTranslation,
        long learnerCount
) {
}
