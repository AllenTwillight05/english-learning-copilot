package com.englishlearningcopilot.backend.service.speech;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Mock ISE service that returns random pronunciation scores (70–95 range).
 * Replace with real iFlytek ISE (语音评测) when API credentials are available.
 */
@Component
public class MockIseService implements IseService {

    @Override
    public PronunciationScore evaluate(byte[] audio, String referenceText) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return new PronunciationScore(
                round1(72 + rng.nextDouble() * 23),   // totalScore: 72–95
                round1(70 + rng.nextDouble() * 25),   // accuracy:   70–95
                round1(68 + rng.nextDouble() * 27),   // fluency:    68–95
                round1(75 + rng.nextDouble() * 20),   // integrity:  75–95
                round1(90 + rng.nextDouble() * 60)     // speed:     90–150 WPM (not used in UI yet)
        );
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
