package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.SpeakingSession;
import java.time.Instant;
import java.util.List;

public record SpeakingSessionResponse(
        Long id,
        Long userId,
        SpeakingScenarioResponse scenario,
        String status,
        Instant startedAt,
        Instant completedAt,
        int currentTurn,
        int targetTurns,
        String selectedTopic,
        List<SpeakingMessageResponse> messages
) {

    public static SpeakingSessionResponse from(SpeakingSession session, List<SpeakingMessageResponse> messages) {
        return new SpeakingSessionResponse(
                session.getId(),
                session.getUser().getId(),
                SpeakingScenarioResponse.from(session.getScenario()),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCurrentTurn(),
                session.getTargetTurns(),
                session.getSelectedTopic(),
                messages
        );
    }
}
