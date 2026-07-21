package com.englishlearningcopilot.backend.dto;

public record DailyPracticeProgressResponse(
        int completed,
        int total,
        int remaining,
        boolean done
) {
}
