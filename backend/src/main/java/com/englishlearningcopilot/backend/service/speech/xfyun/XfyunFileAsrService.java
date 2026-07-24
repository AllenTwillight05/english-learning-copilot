package com.englishlearningcopilot.backend.service.speech.xfyun;

import com.englishlearningcopilot.backend.service.speech.AsrService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "xfyun.asr.enabled", havingValue = "true")
@EnableConfigurationProperties(XfyunFileAsrProperties.class)
public class XfyunFileAsrService implements AsrService {

    private final XfyunFileAsrProperties properties;
    private final XfyunFileAsrClient client;
    private final XfyunTranscriptionParser parser;
    private final XfyunAsrAudioConverter audioConverter;

    public XfyunFileAsrService(
            XfyunFileAsrProperties properties,
            XfyunFileAsrClient client,
            XfyunTranscriptionParser parser,
            XfyunAsrAudioConverter audioConverter
    ) {
        this.properties = properties;
        this.client = client;
        this.parser = parser;
        this.audioConverter = audioConverter;
    }

    @Override
    public String transcribe(byte[] audio) {
        return transcribe(audio, properties.fileName());
    }

    @Override
    public String transcribe(byte[] audio, String fileName) {
        if (audio == null || audio.length == 0) {
            throw new XfyunAsrException("Uploaded audio is empty.");
        }

        byte[] pcmAudio = audioConverter.toPcm16kMono(audio, properties);
        String uploadFileName = properties.transcodeEnabled() ? "recording.pcm" : fileName;
        XfyunFileAsrOrder order = client.upload(pcmAudio, properties, uploadFileName);
        long deadline = System.currentTimeMillis() + properties.timeoutMs();
        while (System.currentTimeMillis() < deadline) {
            XfyunFileAsrResult result = client.getResult(order, properties);
            if (result.isCompleted()) {
                String transcript = parser.parse(result.orderResult());
                if (transcript.isBlank()) {
                    throw new XfyunAsrException("XFYUN ASR completed but returned empty transcript.");
                }
                return transcript;
            }
            if (result.isFailed()) {
                throw new XfyunAsrException(toReadableFailure(result.failType()));
            }
            if (!result.isProcessing()) {
                throw new XfyunAsrException("XFYUN ASR returned unknown order status: " + result.orderStatus());
            }
            sleep();
        }
        throw new XfyunAsrException("XFYUN ASR timed out.");
    }

    private void sleep() {
        try {
            Thread.sleep(properties.pollIntervalMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new XfyunAsrException("XFYUN ASR polling was interrupted.", e);
        }
    }

    String toReadableFailure(String failType) {
        if ("6".equals(failType)) {
            return "讯飞将上传音频判定为静音。请确认麦克风输入和音频转码配置。";
        }
        return "XFYUN ASR failed: " + failType;
    }
}
