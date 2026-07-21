package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import java.util.List;

public record SpeakingFeedbackResponse(
        // ---- original summary fields ----
        int totalScore,
        int pronunciation,
        int fluency,
        String speed,
        List<String> issueSentences,
        List<String> suggestions,

        // ---- new per-turn fields (tech spec §5.2) ----
        String scenarioTitle,
        int totalTurns,
        double averagePronunciationScore,
        List<TurnFeedback> turns,
        String agentOverallComment
) {
}
