package com.englishlearningcopilot.backend.service.speech.xfyun;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xfyun.asr")
public record XfyunFileAsrProperties(
        boolean enabled,
        String baseUrl,
        String appId,
        String apiKey,
        String apiSecret,
        String language,
        String accent,
        String resultType,
        boolean durationCheckDisable,
        int pollIntervalMs,
        int timeoutMs,
        String fileName
) {

    public XfyunFileAsrProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://office-api-ist-dx.iflyaisol.com";
        }
        if (language == null || language.isBlank()) {
            language = "autodialect";
        }
        if (accent == null || accent.isBlank()) {
            accent = "mandarin";
        }
        if (resultType == null || resultType.isBlank()) {
            resultType = "transfer";
        }
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 1000;
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30000;
        }
        if (fileName == null || fileName.isBlank()) {
            fileName = "recording.webm";
        }
    }
}
