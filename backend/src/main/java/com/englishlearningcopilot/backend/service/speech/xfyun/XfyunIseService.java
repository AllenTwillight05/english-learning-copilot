package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.englishlearningcopilot.backend.service.speech.IseService;
import com.englishlearningcopilot.backend.service.speech.PronunciationScore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "xfyun.ise.enabled", havingValue = "true")
@EnableConfigurationProperties(XfyunIseProperties.class)
public class XfyunIseService implements IseService {

    private final XfyunIseProperties properties;
    private final XfyunIseAudioConverter audioConverter;
    private final XfyunIseClient client;

    public XfyunIseService(
            XfyunIseProperties properties,
            XfyunIseAudioConverter audioConverter,
            XfyunIseClient client
    ) {
        this.properties = properties;
        this.audioConverter = audioConverter;
        this.client = client;
    }

    @Override
    public PronunciationScore evaluate(byte[] audio, String referenceText) {
        if (referenceText == null || referenceText.isBlank()) {
            throw new XfyunIseException("XFYUN ISE requires reference text. Run ASR before ISE.");
        }
        byte[] pcmAudio = audioConverter.toPcm16kMono(audio, properties);
        return client.evaluate(pcmAudio, referenceText, properties);
    }
}
