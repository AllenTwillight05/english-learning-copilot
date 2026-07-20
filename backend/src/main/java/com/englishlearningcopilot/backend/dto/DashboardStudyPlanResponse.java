package com.englishlearningcopilot.backend.dto;

public record DashboardStudyPlanResponse(
        DailyPracticeProgressResponse vocabulary,
        DailyPracticeProgressResponse grammar,
        int streakDays,
        boolean allDone
) {
}
