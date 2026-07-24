package com.englishlearningcopilot.backend.service.speech;

/**
 * Intelligent Speech Evaluation — evaluates pronunciation quality of spoken English.
 */
public interface IseService {

    /**
     * Evaluate the pronunciation quality of recorded speech.
     *
     * @param audio         raw audio bytes from the user recording
     * @param referenceText optional reference text for comparison (may be null for free speech)
     * @return pronunciation scores across multiple dimensions
     */
    PronunciationScore evaluate(byte[] audio, String referenceText);
}
