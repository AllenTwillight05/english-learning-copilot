package com.englishlearningcopilot.backend.dto;

import com.englishlearningcopilot.backend.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonProperty;

public record VocabularyWordbookWordResponse(
        Long id,
        String word,
        String phonetic,
        String definition,
        String briefTranslation,
        String translation,
        String tag,
        @JsonProperty("us_audio") String usAudio,
        boolean favorited
) {

    public static VocabularyWordbookWordResponse from(Vocabulary vocabulary, boolean favorited) {
        return new VocabularyWordbookWordResponse(
                vocabulary.getId(),
                vocabulary.getWord(),
                vocabulary.getPhonetic(),
                vocabulary.getDefinition(),
                nullToEmpty(vocabulary.getBriefTranslation()),
                vocabulary.getTranslation(),
                vocabulary.getTag(),
                vocabulary.getUsAudio(),
                favorited
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
