package com.englishlearningcopilot.backend.dto;

public record LearningPlanResponse(
        int dailyVocabularyGoal,
        int dailyGrammarGoal,
        boolean enabled
) {
}
