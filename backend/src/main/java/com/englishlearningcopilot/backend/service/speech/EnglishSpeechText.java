package com.englishlearningcopilot.backend.service.speech;

/**
 * Determines whether an ASR transcript is suitable for the English ISE service.
 */
public final class EnglishSpeechText {

    private EnglishSpeechText() {
    }

    public static boolean isEligibleForPronunciationEvaluation(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return false;
        }

        int latinLetters = 0;
        for (int offset = 0; offset < transcript.length();) {
            int codePoint = transcript.codePointAt(offset);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                return false;
            }
            if (codePoint <= Character.MAX_VALUE && Character.isLetter((char) codePoint)
                    && script == Character.UnicodeScript.LATIN) {
                latinLetters++;
            }
            offset += Character.charCount(codePoint);
        }
        return latinLetters > 0;
    }
}
