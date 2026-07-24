package com.englishlearningcopilot.backend.service.speech.xfyun;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supersmart.tts")
public record XfyunOnlineTtsProperties(
        boolean enabled,
        String url,
        String appId,
        String apiKey,
        String apiSecret,
        String voice,
        String audioEncoding,
        String textEncoding,
        int sampleRate,
        int speed,
        int pitch,
        int volume,
        int timeoutMs
) {

    public XfyunOnlineTtsProperties {
        if (url == null || url.isBlank()) {
            url = "wss://cbm01.cn-huabei-1.xf-yun.com/v1/private/mcd9m97e6";
        }
        if (voice == null || voice.isBlank()) {
            voice = "x6_lingxiaoxuan_pro";
        }
        if (audioEncoding == null || audioEncoding.isBlank()) {
            audioEncoding = "lame";
        }
        if (textEncoding == null || textEncoding.isBlank()) {
            textEncoding = "utf8";
        }
        if (sampleRate <= 0) {
            sampleRate = 24000;
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
