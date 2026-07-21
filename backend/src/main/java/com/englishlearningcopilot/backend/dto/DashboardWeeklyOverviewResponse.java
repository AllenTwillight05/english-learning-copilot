package com.englishlearningcopilot.backend.dto;

public record DashboardWeeklyOverviewResponse(
        String speakingDuration,
        String pronunciationAccuracy,
        String learningDays,
        String vocabularyLearned,
        String grammarPracticed
) {
}
