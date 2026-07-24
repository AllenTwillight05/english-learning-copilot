package com.englishlearningcopilot.backend.dto;

import java.util.List;

public record ProfileSnapshotResponse(
        String learnerName,
        String level,
        String streak,
        FeedbackSummary feedback,
        ProfileDailyPlan dailyPlan
) {

    public record FeedbackSummary(
            String statusLabel,
            String playbackActionLabel,
            List<FeedbackMetric> metrics,
            List<String> notes,
            String scenarioTitle,
            String completedAt,
            Integer totalScore,
            Integer pronunciation,
            Integer fluency,
            Integer integrity,
            String issueSentence
    ) {
    }

    public record FeedbackMetric(
            String key,
            String label,
            String value
    ) {
    }

    public record ProfileDailyPlan(
            boolean autoPilotEnabled,
            String weeklyImprovement,
            int dailyVocabularyGoal,
            int dailyGrammarGoal,
            DailyPracticeProgressResponse vocabulary,
            DailyPracticeProgressResponse grammar,
            boolean allDone,
            List<PlanItem> items,
            List<ProgressMetric> progress
    ) {
    }

    public record PlanItem(
            String id,
            String time,
            String task,
            String meta,
            boolean done
    ) {
    }

    public record ProgressMetric(
            String id,
            String label,
            int value,
            String tone
    ) {
    }
}
