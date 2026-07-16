package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.SpeakingMessage;
import java.time.Instant;

public record SpeakingMessageResponse(
        Long id,
        String sender,
        String content,
        String instantTip,
        int turnIndex,
        Instant createdAt
) {

    public static SpeakingMessageResponse from(SpeakingMessage message) {
        return new SpeakingMessageResponse(
                message.getId(),
                message.getSender().name(),
                message.getContent(),
                message.getInstantTip(),
                message.getTurnIndex(),
                message.getCreatedAt()
        );
    }
}
