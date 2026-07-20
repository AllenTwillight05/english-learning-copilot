package com.englishlearningcopilot.backend.service.speech;

/**
 * Pronunciation evaluation score returned by ISE service.
 */
public record PronunciationScore(
        double totalScore,
        double accuracy,
        double fluency,
        double integrity,
        double speed
) {
}
