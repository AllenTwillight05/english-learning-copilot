package com.englishlearningcopilot.backend.dto;

import java.util.List;

public record DashboardCommunityLearningTrendsResponse(
        List<DashboardSpeakingTrendResponse> speaking,
        List<VocabularyLeaderboardItemResponse> vocabulary,
        List<DashboardGrammarTrendResponse> grammar
) {
}
