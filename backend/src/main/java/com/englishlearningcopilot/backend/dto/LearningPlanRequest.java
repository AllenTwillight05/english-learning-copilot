package com.englishlearningcopilot.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LearningPlanRequest(
        @NotNull @Min(0) @Max(200) Integer dailyVocabularyGoal,
        @NotNull @Min(0) @Max(100) Integer dailyGrammarGoal
) {
}
