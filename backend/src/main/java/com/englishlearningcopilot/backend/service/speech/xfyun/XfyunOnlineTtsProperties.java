package com.englishlearningcopilot.backend.service.speech.xfyun;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xfyun.tts")
public record XfyunOnlineTtsProperties(
        boolean enabled,
        String url,
        String appId,
        String apiKey,
        String apiSecret,
        String voice,
        String audioEncoding,
        String textEncoding,
        int speed,
        int pitch,
        int volume,
        int timeoutMs
) {

    public XfyunOnlineTtsProperties {
        if (url == null || url.isBlank()) {
            url = "wss://tts-api.xfyun.cn/v2/tts";
        }
        if (voice == null || voice.isBlank()) {
            voice = "xiaoyan";
        }
        if (audioEncoding == null || audioEncoding.isBlank()) {
            audioEncoding = "lame";
        }
        if (textEncoding == null || textEncoding.isBlank()) {
            textEncoding = "utf8";
        }
        if (speed <= 0) {
            speed = 50;
        }
        if (pitch <= 0) {
            pitch = 50;
        }
        if (volume <= 0) {
            volume = 50;
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30000;
        }
    }
}
