package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.englishlearningcopilot.backend.service.speech.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "xfyun.tts.enabled", havingValue = "true")
@EnableConfigurationProperties(XfyunOnlineTtsProperties.class)
public class XfyunOnlineTtsService implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(XfyunOnlineTtsService.class);

    private final XfyunOnlineTtsProperties properties;
    private final XfyunOnlineTtsClient client;

    public XfyunOnlineTtsService(XfyunOnlineTtsProperties properties, XfyunOnlineTtsClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            return new byte[0];
        }

        try {
            return client.synthesize(text, properties);
        } catch (RuntimeException e) {
            log.warn("XFYUN Super Smart TTS synthesis failed. Returning empty audio for browser fallback.", e);
            return new byte[0];
        }
    }
}
