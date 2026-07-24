package com.englishlearningcopilot.backend.service.speech;

/**
 * Text-to-Speech — converts text to spoken audio.
 */
public interface TtsService {

    /**
     * Synthesize English text into audio.
     *
     * @param text the English text to speak
     * @return audio bytes (e.g. MP3), or empty array if synthesis is unavailable
     */
    byte[] synthesize(String text);
}
