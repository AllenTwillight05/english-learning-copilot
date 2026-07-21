package com.englishlearningcopilot.backend.service.speech.xfyun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class XfyunFileAsrServiceTest {

    @Test
    void mapsSilentAudioFailureToReadableMessage() {
        XfyunFileAsrService service = new XfyunFileAsrService(null, null, null);

        assertThat(service.toReadableFailure("6")).isEqualTo("没有识别到语音");
    }
}
