package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import java.util.Arrays;
import java.util.List;

public record SpeakingScenarioResponse(
        String id,
        String title,
        String description,
        String difficulty,
        String level,
        String accent,
        String duration,
        String summary,
        String tone,
        String goal,
        List<String> keywords,
        String openingMessage,
        String sampleDialogue,
        int targetTurns,
        String scoringRubric
) {

    public static SpeakingScenarioResponse from(SpeakingScenario scenario) {
        return new SpeakingScenarioResponse(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getDescription(),
                scenario.getDifficulty(),
                scenario.getDifficulty(),
                scenario.getAccent(),
                scenario.getDuration(),
                scenario.getSummary(),
                scenario.getTone(),
                scenario.getGoal(),
                splitKeywords(scenario.getKeywords()),
                scenario.getOpeningMessage(),
                scenario.getSampleDialogue(),
                scenario.getTargetTurns(),
                scenario.getScoringRubric()
        );
    }

    private static List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .toList();
    }
}
