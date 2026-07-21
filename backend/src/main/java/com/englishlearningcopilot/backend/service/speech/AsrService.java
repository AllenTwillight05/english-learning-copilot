package com.englishlearningcopilot.backend.service.speech;

/**
 * Automatic Speech Recognition — converts audio to text.
 */
public interface AsrService {

    /**
     * Transcribe audio bytes to English text.
     *
     * @param audio raw audio bytes (e.g. WebM/Opus from browser MediaRecorder)
     * @return transcribed English text
     */
    String transcribe(byte[] audio);
}
