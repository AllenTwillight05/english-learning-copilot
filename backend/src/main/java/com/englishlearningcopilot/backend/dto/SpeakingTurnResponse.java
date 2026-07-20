package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.service.speech.PronunciationScore;

public record SpeakingTurnResponse(
        SpeakingMessageResponse userMessage,
        SpeakingMessageResponse agentMessage,
        PronunciationScore pronunciationScore,
        SpeakingSessionResponse session
) {
}
