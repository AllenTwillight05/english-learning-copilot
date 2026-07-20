package com.englishlearningcopilot.backend.service.speech;

import org.springframework.stereotype.Component;

/**
 * Mock TTS service that returns an empty audio array.
 * The frontend falls back to browser SpeechSynthesis when audioUrl is empty.
 * Replace with real iFlytek TTS (语音合成) when API credentials are available.
 */
@Component
public class MockTtsService implements TtsService {

    @Override
    public byte[] synthesize(String text) {
        return new byte[0];
    }
}
