package com.englishlearningcopilot.backend.dto;

import java.time.LocalDate;

public record DailyLearningStatusResponse(
        LocalDate date,
        DailyPracticeProgressResponse vocabulary,
        DailyPracticeProgressResponse grammar,
        boolean allDone,
        int streakDays
) {
}
