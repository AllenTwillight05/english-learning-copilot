package com.englishlearningcopilot.backend.service.speech.xfyun;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xfyun.ise")
public record XfyunIseProperties(
        boolean enabled,
        String url,
        String appId,
        String apiKey,
        String apiSecret,
        String language,
        String category,
        int sampleRate,
        int timeoutMs,
        int audioFrameBytes,
        boolean transcodeEnabled,
        String transcodeCommand
) {

    public XfyunIseProperties {
        if (url == null || url.isBlank()) {
            url = "wss://ise-api.xfyun.cn/v2/open-ise";
        }
        if (language == null || language.isBlank()) {
            language = "en_vip";
        }
        if (category == null || category.isBlank()) {
            category = "read_sentence";
        }
        if (sampleRate <= 0) {
            sampleRate = 16000;
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30000;
        }
        if (audioFrameBytes <= 0) {
            audioFrameBytes = 1280;
        }
        if (transcodeCommand == null || transcodeCommand.isBlank()) {
            transcodeCommand = "ffmpeg";
        }
    }

    public String audioFormat() {
        return sampleRate == 8000 ? "audio/L16;rate=8000" : "audio/L16;rate=16000";
    }
}
