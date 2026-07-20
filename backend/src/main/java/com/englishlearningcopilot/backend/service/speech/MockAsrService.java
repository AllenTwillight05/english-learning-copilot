package com.englishlearningcopilot.backend.service.speech;

import org.springframework.stereotype.Component;

/**
 * Mock ASR service that returns a placeholder transcript.
 * Replace with real iFlytek ASR (语音听写) when API credentials are available.
 */
@Component
public class MockAsrService implements AsrService {

    @Override
    public String transcribe(byte[] audio) {
        return "This is a mock transcript of the recorded speech.";
    }
}
