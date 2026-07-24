package com.englishlearningcopilot.backend.dto;

public record DashboardSpeakingTrendResponse(
        int rank,
        String scenarioId,
        String topic,
        String description,
        long learnerCount
) {
}
