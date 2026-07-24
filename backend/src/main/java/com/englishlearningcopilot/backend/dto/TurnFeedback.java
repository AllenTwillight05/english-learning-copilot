package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.service.speech.PronunciationScore;

public record TurnFeedback(
        int turnIndex,
        String userText,
        String agentText,
        PronunciationScore score
) {
}
