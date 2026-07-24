package com.englishlearningcopilot.backend.service.agent;

public record SpeakingAgentReply(
        String content,
        String spokenText,
        String instantTip
) {
    public static SpeakingAgentReply of(String content, String instantTip) {
        return new SpeakingAgentReply(content, content, instantTip);
    }
}
