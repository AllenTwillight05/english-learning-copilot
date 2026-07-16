package com.englishlearningcopilot.backend.dto;

public record SpeakingTurnResponse(
        SpeakingMessageResponse userMessage,
        SpeakingMessageResponse agentMessage,
        SpeakingSessionResponse session
) {
}
