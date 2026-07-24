package com.englishlearningcopilot.backend.service.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "speaking.agent.sjtu")
public record SjtuSpeakingAgentProperties(
        String endpoint,
        String apiKey,
        String model,
        double temperature,
        int maxTokens,
        int timeoutMs,
        String promptLabDir,
        int historyMessageLimit,
        int historyContextMaxChars
) {

    public SjtuSpeakingAgentProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "https://models.sjtu.edu.cn/api/v1";
        }
        endpoint = endpoint.replaceAll("/+$", "");
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }
        if (temperature <= 0) {
            temperature = 0.7;
        }
        if (maxTokens <= 0) {
            maxTokens = 120;
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30000;
        }
        if (promptLabDir == null || promptLabDir.isBlank()) {
            promptLabDir = "llm-prompt-lab";
        }
        if (historyMessageLimit <= 0) {
            historyMessageLimit = 8;
        }
        if (historyContextMaxChars <= 0) {
            historyContextMaxChars = 600;
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
