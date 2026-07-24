package com.englishlearningcopilot.backend.dto;

public record DashboardGrammarTrendResponse(
        int rank,
        String grammarCategory,
        long learnerCount
) {
}
